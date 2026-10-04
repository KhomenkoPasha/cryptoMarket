package app.khom.pavlo.crypto.model.db

import app.khom.pavlo.crypto.model.PortfolioHistoryBuilder
import app.khom.pavlo.crypto.model.PortfolioSnapshot
import app.khom.pavlo.crypto.model.Preferences
import app.khom.pavlo.crypto.model.network.NetworkRequests
import io.reactivex.rxjava3.core.Flowable

/**
 * Keeps one snapshot per portfolio per day. History is rebuilt from the transactions and past
 * prices whenever they changed, and otherwise just extended with the days that are missing.
 */
class PortfolioHistoryRepository(
    private val db: CMDatabase,
    private val networkRequests: NetworkRequests,
    private val preferences: Preferences
) {

    fun observeSnapshots(): Flowable<List<PortfolioSnapshot>> =
        db.portfolioSnapshotsDao().observeAll().distinctUntilChanged()

    fun markDirty() {
        preferences.historyDirty = true
    }

    /** Brings every portfolio's history up to today. Blocks, so call it away from the main thread. */
    @Synchronized
    fun refreshBlocking(now: Long = System.currentTimeMillis()): Boolean {
        val dao = db.portfolioSnapshotsDao()
        val today = PortfolioHistoryBuilder.dayOf(now)
        val wasDirty = preferences.historyDirty
        val holdingsByPortfolio = db.holdingsDao().getAllHoldingsSync().groupBy { it.portfolioId }

        dao.deleteOrphansSync()
        // Portfolios that lost all their transactions have nothing left to chart.
        db.portfoliosDao().getAllSync()
            .filter { it.id !in holdingsByPortfolio.keys }
            .forEach { dao.deleteForPortfolioSync(it.id) }

        var complete = true
        holdingsByPortfolio.forEach { (portfolioId, transactions) ->
            val existing = dao.getForPortfolioSync(portfolioId)
            val firstDay = transactions.minOf { PortfolioHistoryBuilder.dayOf(it.date) }
            val startDay = maxOf(firstDay, today - MAX_HISTORY_DAYS + 1)
            val rebuild = wasDirty || existing.isEmpty() || existing.first().day != startDay
            // Without a rebuild only the last stored day (it was still moving) and the new days are redone.
            val fromDay = if (rebuild) startDay else maxOf(existing.last().day, startDay)

            val symbols = transactions.map { it.from }.distinct()
            val daysNeeded = (today - fromDay + 2).toInt()
            val prices = HashMap<String, Map<Long, Double>>()
            var priced = true
            symbols.forEach { symbol ->
                val series = runCatching { networkRequests.getDailyCloses(symbol, daysNeeded).blockingGet() }
                    .getOrNull()
                if (series == null || series.isEmpty()) priced = false else prices[symbol] = series
            }
            // Writing history built without prices would replace good data with a cost-based guess.
            if (!priced) {
                complete = false
                return@forEach
            }

            val snapshots = PortfolioHistoryBuilder.build(portfolioId, transactions, prices, fromDay, today)
            if (rebuild) dao.replaceForPortfolio(portfolioId, snapshots) else dao.insertAllSync(snapshots)
        }
        if (complete) preferences.historyDirty = false
        return complete
    }

    companion object {
        /** Two years of daily points is plenty for a chart and keeps requests small. */
        const val MAX_HISTORY_DAYS = 730L
    }
}
