package app.khom.pavlo.crypto.model

import java.math.BigDecimal

/** Rebuilds how a portfolio was worth day by day from its transactions and historical prices. */
object PortfolioHistoryBuilder {

    const val DAY_MILLIS = 86_400_000L

    fun dayOf(epochMillis: Long): Long = Math.floorDiv(epochMillis, DAY_MILLIS)

    /**
     * @param prices closing price in USD by symbol and day. A day without a price carries the last
     * earlier close forward, and days before a coin's first price use that first close.
     * @return one snapshot per day from the later of [fromDay] and the first transaction up to [toDay]
     */
    fun build(
        portfolioId: Long,
        transactions: List<HoldingData>,
        prices: Map<String, Map<Long, Double>>,
        fromDay: Long,
        toDay: Long
    ): List<PortfolioSnapshot> {
        if (transactions.isEmpty()) return emptyList()
        val firstDay = transactions.minOf { dayOf(it.date) }
        val sortedPrices = prices.mapValues { (_, series) -> PriceSeries(series) }
        val snapshots = ArrayList<PortfolioSnapshot>()

        for (day in maxOf(fromDay, firstDay)..toDay) {
            val upToToday = transactions.filter { dayOf(it.date) <= day }
            val positions = PositionCalculator.replay(upToToday).positions

            var value = BigDecimal.ZERO
            var invested = BigDecimal.ZERO
            positions.forEach { position ->
                invested += position.costBasis
                if (position.quantity.signum() > 0) {
                    val price = sortedPrices[position.from]?.closeAt(day)
                    // Without any price for the coin the cost is the best estimate of its worth.
                    value += if (price != null) position.quantity.multiply(price) else position.costBasis
                }
            }

            var flow = BigDecimal.ZERO
            transactions.filter { dayOf(it.date) == day }.forEach { transaction ->
                val marketPrice = sortedPrices[transaction.from]?.closeAt(day) ?: transaction.price
                flow += when (transaction.tradeType) {
                    TradeType.BUY -> transaction.quantity.multiply(transaction.price) + transaction.fee
                    TradeType.SELL -> -(transaction.quantity.multiply(transaction.price) - transaction.fee)
                    TradeType.TRANSFER_IN -> transaction.quantity.multiply(marketPrice)
                    TradeType.TRANSFER_OUT -> -transaction.quantity.multiply(marketPrice)
                }
            }
            snapshots.add(PortfolioSnapshot(portfolioId, day, value, invested, flow))
        }
        return snapshots
    }

    private class PriceSeries(series: Map<Long, Double>) {
        private val days = series.keys.sorted().toLongArray()
        private val closes = days.map { series.getValue(it) }.toDoubleArray()

        fun closeAt(day: Long): BigDecimal? {
            if (days.isEmpty()) return null
            var low = 0
            var high = days.size - 1
            var index = -1
            while (low <= high) {
                val mid = (low + high) ushr 1
                if (days[mid] <= day) {
                    index = mid
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }
            val close = closes[if (index >= 0) index else 0]
            return close.takeIf { it.isFinite() && it > 0.0 }?.let(BigDecimal::valueOf)
        }
    }
}

data class PerformancePoint(
    val day: Long,
    val value: BigDecimal,
    val netFlow: BigDecimal
)

data class DayReturn(val day: Long, val percent: Double)

data class PerformanceStats(
    /** Largest fall from a previous high, in percent (zero or positive). */
    val maxDrawdownPercent: Double,
    val bestDay: DayReturn?,
    val worstDay: DayReturn?,
    /** Growth over the period with deposits and withdrawals taken out, in percent. */
    val periodReturnPercent: Double?
)

/**
 * Judges how the investments did, not how much money was added. Every day's return compares the
 * value with the day before after removing that day's deposits and withdrawals, and chaining the
 * returns gives a curve that deposits cannot lift or withdrawals cannot sink.
 */
object PortfolioPerformance {

    fun analyze(points: List<PerformancePoint>): PerformanceStats {
        val ordered = points.sortedBy { it.day }
        var index = 1.0
        var peak = 1.0
        var maxDrawdown = 0.0
        var best: DayReturn? = null
        var worst: DayReturn? = null
        var hasReturns = false

        for (i in 1 until ordered.size) {
            val previous = ordered[i - 1].value
            // A day with no previous value has nothing to measure a return against.
            if (previous.signum() <= 0) continue
            val current = ordered[i]
            val growth = (current.value - current.netFlow).toDouble() / previous.toDouble()
            if (!growth.isFinite()) continue
            val dayReturn = DayReturn(current.day, (growth - 1.0) * 100.0)
            hasReturns = true
            index *= growth
            peak = maxOf(peak, index)
            maxDrawdown = maxOf(maxDrawdown, (peak - index) / peak * 100.0)
            if (best == null || dayReturn.percent > best.percent) best = dayReturn
            if (worst == null || dayReturn.percent < worst.percent) worst = dayReturn
        }
        return PerformanceStats(
            maxDrawdownPercent = maxDrawdown,
            bestDay = best,
            worstDay = worst,
            periodReturnPercent = if (hasReturns) (index - 1.0) * 100.0 else null
        )
    }
}
