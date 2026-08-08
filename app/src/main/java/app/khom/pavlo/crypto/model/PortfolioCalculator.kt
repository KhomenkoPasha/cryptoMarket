package app.khom.pavlo.crypto.model

import java.math.BigDecimal
import java.math.MathContext

object PortfolioCalculator {

    private val zero = BigDecimal.ZERO
    private val oneHundred = BigDecimal("100")
    private val mathContext = MathContext.DECIMAL128

    fun summary(holdings: List<HoldingData>, coins: List<Coin>): PortfolioSummary {
        val investedValue = investedValue(holdings)
        val currentValue = currentValue(holdings, coins)
        val dayPnl = totalDayPnl(holdings, coins)
        val previousValue = currentValue - dayPnl

        return PortfolioSummary(
            investedValue = investedValue,
            currentValue = currentValue,
            totalPnl = currentValue - investedValue,
            totalPnlPercent = changePercent(investedValue, currentValue),
            dayPnl = dayPnl,
            dayPnlPercent = changePercent(previousValue, currentValue)
        )
    }

    fun statsFor(
        holding: HoldingData,
        holdings: List<HoldingData>,
        coins: List<Coin>
    ): PortfolioHoldingStats {
        val matching = holdings.filter { it.from == holding.from && it.to == holding.to }
        val quantity = matching.sumOf { it.quantity }
        val invested = investedValue(matching)
        val averageBuyPrice = if (quantity.signum() > 0) {
            invested.divide(quantity, mathContext)
        } else {
            zero
        }
        val pairCurrentValue = currentValue(matching, coins)
        val totalCurrentValue = currentValue(holdings, coins)
        val coin = coinFor(holding, coins)

        return PortfolioHoldingStats(
            averageBuyPrice = averageBuyPrice,
            allocationPercent = if (totalCurrentValue.signum() > 0) {
                pairCurrentValue.multiply(oneHundred).divide(totalCurrentValue, mathContext)
            } else {
                zero
            },
            dayPnl = dayPnl(matching, coins),
            dayPnlPercent = coin?.changePct24hRaw?.toDecimal() ?: zero
        )
    }

    fun investedValue(holdings: List<HoldingData>): BigDecimal =
        holdings.sumOf { it.quantity.multiply(it.price) }

    fun currentValue(holdings: List<HoldingData>, coins: List<Coin>): BigDecimal =
        holdings.sumOf { holding ->
            val currentPrice = coinFor(holding, coins)?.priceRaw?.toDecimal() ?: zero
            holding.quantity.multiply(currentPrice)
        }

    fun currentValue(holding: HoldingData, coins: List<Coin>): BigDecimal =
        currentValue(listOf(holding), coins)

    fun changePercent(holding: HoldingData, coins: List<Coin>): BigDecimal {
        val oldValue = holding.quantity.multiply(holding.price)
        return changePercent(oldValue, currentValue(holding, coins))
    }

    fun changeValue(holding: HoldingData, coins: List<Coin>): BigDecimal =
        currentValue(holding, coins) - holding.quantity.multiply(holding.price)

    fun aggregatePair(holding: HoldingData, holdings: List<HoldingData>): HoldingData {
        val matching = holdings.filter { it.from == holding.from && it.to == holding.to }
        val quantity = matching.sumOf { it.quantity }
        val invested = investedValue(matching)
        val averagePrice = if (quantity.signum() > 0) invested.divide(quantity, mathContext) else zero
        return holding.copy(
            quantity = quantity,
            price = averagePrice,
            date = matching.minOfOrNull { it.date } ?: holding.date
        )
    }

    private fun totalDayPnl(holdings: List<HoldingData>, coins: List<Coin>): BigDecimal =
        holdings
            .groupBy { it.from to it.to }
            .values
            .sumOf { dayPnl(it, coins) }

    private fun dayPnl(holdings: List<HoldingData>, coins: List<Coin>): BigDecimal {
        val first = holdings.firstOrNull() ?: return zero
        val coin = coinFor(first, coins) ?: return zero
        val current = currentValue(holdings, coins)
        val denominator = BigDecimal.ONE + coin.changePct24hRaw.toDecimal().divide(oneHundred, mathContext)
        if (denominator.signum() <= 0) return zero
        val previous = current.divide(denominator, mathContext)
        return current - previous
    }

    private fun changePercent(oldValue: BigDecimal, newValue: BigDecimal): BigDecimal {
        if (oldValue.signum() == 0) return zero
        return (newValue - oldValue).multiply(oneHundred).divide(oldValue, mathContext)
    }

    private fun coinFor(holding: HoldingData, coins: List<Coin>): Coin? =
        coins.find { it.from == holding.from && it.to == holding.to }
            ?: coins.find { it.from == holding.from }

    private fun Float.toDecimal(): BigDecimal = BigDecimal.valueOf(toDouble())
}
