package app.khom.pavlo.crypto.model.db

import app.khom.pavlo.crypto.model.ALL_PORTFOLIOS_ID
import app.khom.pavlo.crypto.model.DEFAULT_PORTFOLIO_ID
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.Portfolio
import app.khom.pavlo.crypto.model.PortfolioChangeNotifier
import app.khom.pavlo.crypto.model.PortfolioSelection
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

class PortfolioRepository(
    private val db: CMDatabase,
    private val changeNotifier: PortfolioChangeNotifier,
    val selection: PortfolioSelection,
    /** Called after transactions are added, edited or removed, so derived data can be refreshed. */
    private val onDataChanged: () -> Unit = {}
) {

    /** Transactions of the portfolio currently selected, or of all of them. */
    fun observeHoldings(): Flowable<List<HoldingData>> =
        Flowable.combineLatest(
            db.holdingsDao().getAllHoldings(),
            selection.observe()
        ) { all, selected ->
            if (selected == ALL_PORTFOLIOS_ID) all else all.filter { it.portfolioId == selected }
        }.distinctUntilChanged()

    /** Every transaction regardless of the selected portfolio. */
    fun observeAllHoldings(): Flowable<List<HoldingData>> =
        db.holdingsDao().getAllHoldings().distinctUntilChanged()

    fun addHolding(holding: HoldingData): Completable =
        db.holdingsDao().insert(holding).notifyDataChanged()

    fun getHolding(id: Long): Single<HoldingData> = db.holdingsDao().getById(id)

    /** Every transaction, newest first, read once. */
    fun loadHoldings(): Single<List<HoldingData>> =
        Single.fromCallable { db.holdingsDao().getAllHoldingsSync() }

    /** Adds many transactions at once, for example from a CSV file. Returns how many were stored. */
    fun importHoldings(holdings: List<HoldingData>): Single<Int> =
        Single.fromCallable {
            db.runInTransaction { db.holdingsDao().insertAllSync(holdings) }
            holdings.size
        }.doOnSuccess {
            changeNotifier.onPortfolioChanged()
            onDataChanged()
        }

    fun updateHolding(holding: HoldingData): Completable =
        db.holdingsDao().update(holding).notifyDataChanged()

    fun deleteHolding(holding: HoldingData): Completable =
        db.holdingsDao().deleteHolding(holding).notifyDataChanged()

    fun observePortfolios(): Flowable<List<Portfolio>> =
        db.portfoliosDao().observeAll().distinctUntilChanged()

    fun loadPortfolios(): Single<List<Portfolio>> =
        Single.fromCallable { db.portfoliosDao().getAllSync() }

    fun addPortfolio(name: String): Single<Long> =
        Single.fromCallable {
            val dao = db.portfoliosDao()
            dao.insertSync(
                Portfolio(
                    name = name.trim(),
                    createdAt = System.currentTimeMillis(),
                    sortOrder = dao.nextSortOrderSync()
                )
            )
        }

    fun renamePortfolio(id: Long, name: String): Completable =
        db.portfoliosDao().rename(id, name.trim()).notifyWidget()

    /** Deletes a portfolio together with its transactions. The last portfolio can't be deleted. */
    fun deletePortfolio(id: Long): Completable = Completable.fromAction {
        db.runInTransaction {
            if (db.portfoliosDao().countSync() <= 1) {
                throw IllegalStateException("At least one portfolio is required")
            }
            db.holdingsDao().deleteByPortfolioSync(id)
            db.portfoliosDao().deleteByIdSync(id)
        }
        if (selection.activeId == id) selection.select(ALL_PORTFOLIOS_ID)
    }.notifyDataChanged()

    /** Selects what to look at and refreshes everything that shows the selected portfolio. */
    fun select(id: Long) {
        selection.select(id)
        changeNotifier.onPortfolioChanged()
    }

    /** Makes sure the built-in portfolio exists, e.g. on a fresh install or after a restore. */
    fun ensureDefaultPortfolio() {
        db.portfoliosDao().insertIfMissingSync(
            Portfolio(id = DEFAULT_PORTFOLIO_ID, createdAt = System.currentTimeMillis(), sortOrder = 0)
        )
    }

    private fun Completable.notifyWidget(): Completable = doOnComplete {
        changeNotifier.onPortfolioChanged()
    }

    private fun Completable.notifyDataChanged(): Completable = doOnComplete {
        changeNotifier.onPortfolioChanged()
        onDataChanged()
    }
}
