package app.khom.pavlo.crypto.model

import java.math.BigDecimal
import java.math.MathContext

object PortfolioCalculator {

    private val zero = BigDecimal.ZERO
    private val oneHundred = BigDecimal("100")
    private val mathContext = MathContext.DECIMAL128

    fun summary(holdings: List<HoldingData>, coins: List<Coin>): PortfolioSummary {
        val positions = PositionCalculator.replay(holdings).positions
        val investedValue = positions.sumOf { it.costBasis }
        val currentValue = positions.sumOf { it.quantity.multiply(currentPrice(it.from, it.to, coins)) }
        val unrealizedPnl = currentValue - investedValue
        val realizedPnl = positions.sumOf { it.realizedPnl }
        val totalPnl = unrealizedPnl + realizedPnl
        val dayPnl = positions.sumOf { dayPnl(it, coins) }
        val previousValue = currentValue - dayPnl
        // Sold coins count towards the base so the percentage still relates profit to money put in.
        val costBase = investedValue + positions.sumOf { it.soldCostBasis }

        return PortfolioSummary(
            investedValue = investedValue,
            currentValue = currentValue,
            totalPnl = totalPnl,
            totalPnlPercent = changePercent(costBase, costBase + totalPnl),
            dayPnl = dayPnl,
            dayPnlPercent = changePercent(previousValue, currentValue),
            realizedPnl = realizedPnl,
            unrealizedPnl = unrealizedPnl,
            feesPaid = positions.sumOf { it.fees },
            hasSales = positions.any { it.soldCostBasis.signum() > 0 || it.realizedPnl.signum() != 0 }
        )
    }

    fun statsFor(
        holding: HoldingData,
        holdings: List<HoldingData>,
        coins: List<Coin>
    ): PortfolioHoldingStats {
        val report = PositionCalculator.replay(holdings)
        val position = report.positionFor(holding.from, holding.to)
        val totalCurrentValue = report.positions.sumOf { it.quantity.multiply(currentPrice(it.from, it.to, coins)) }
        val pairCurrentValue = position?.quantity?.multiply(currentPrice(holding.from, holding.to, coins)) ?: zero
        val coin = coinFor(holding.from, holding.to, coins)

        return PortfolioHoldingStats(
            averageBuyPrice = position?.averageCost ?: zero,
            allocationPercent = if (totalCurrentValue.signum() > 0) {
                pairCurrentValue.multiply(oneHundred).divide(totalCurrentValue, mathContext)
            } else {
                zero
            },
            dayPnl = position?.let { dayPnl(it, coins) } ?: zero,
            dayPnlPercent = coin?.changePct24hRaw?.toDecimal() ?: zero
        )
    }

    /** Stats for a single transaction on its own, as if it were a plain purchase. */
    fun transactionStats(
        holding: HoldingData,
        coins: List<Coin>
    ): PortfolioTransactionStats = transactionStats(holding, listOf(holding), coins)

    /**
     * Stats for one transaction card. [holdings] must hold every transaction of the portfolio so a
     * sale can be matched with the average cost of the coins that were held when it happened.
     */
    fun transactionStats(
        holding: HoldingData,
        holdings: List<HoldingData>,
        coins: List<Coin>
    ): PortfolioTransactionStats {
        val currentPrice = currentPrice(holding.from, holding.to, coins)
        val gross = holding.quantity.multiply(holding.price)
        val currentValue = holding.quantity.multiply(currentPrice)
        return when (holding.tradeType) {
            TradeType.SELL -> {
                val sale = PositionCalculator.replay(holdings).sales[holding.id]
                    ?: SaleResult(gross - holding.fee, zero, gross - holding.fee)
                PortfolioTransactionStats(
                    currentPrice = currentPrice,
                    totalSpent = sale.proceeds,
                    currentValue = currentValue,
                    profit = sale.realizedPnl,
                    profitPercent = changePercent(sale.costBasis, sale.costBasis + sale.realizedPnl),
                    type = TradeType.SELL,
                    fee = holding.fee
                )
            }
            TradeType.TRANSFER_OUT -> PortfolioTransactionStats(
                currentPrice = currentPrice,
                totalSpent = zero,
                currentValue = currentValue,
                profit = zero,
                profitPercent = zero,
                type = TradeType.TRANSFER_OUT,
                fee = zero
            )
            else -> {
                val spent = gross + holding.fee
                val profit = currentValue - spent
                PortfolioTransactionStats(
                    currentPrice = currentPrice,
                    totalSpent = spent,
                    currentValue = currentValue,
                    profit = profit,
                    profitPercent = changePercent(spent, currentValue),
                    type = holding.tradeType,
                    fee = holding.fee
                )
            }
        }
    }

    fun investedValue(holdings: List<HoldingData>): BigDecimal =
        PositionCalculator.replay(holdings).positions.sumOf { it.costBasis }

    fun currentValue(holdings: List<HoldingData>, coins: List<Coin>): BigDecimal =
        PositionCalculator.replay(holdings).positions.sumOf {
            it.quantity.multiply(currentPrice(it.from, it.to, coins))
        }

    fun currentValue(holding: HoldingData, coins: List<Coin>): BigDecimal =
        currentValue(listOf(holding), coins)

    fun changePercent(holding: HoldingData, coins: List<Coin>): BigDecimal {
        val oldValue = holding.quantity.multiply(holding.price) + holding.fee
        return changePercent(oldValue, currentValue(holding, coins))
    }

    fun changeValue(holding: HoldingData, coins: List<Coin>): BigDecimal =
        currentValue(holding, coins) - (holding.quantity.multiply(holding.price) + holding.fee)

    /**
     * Merges every transaction of the holding's coin into one: the net quantity held and its
     * average cost. A coin that was sold completely ends up with a zero quantity.
     */
    fun aggregatePair(holding: HoldingData, holdings: List<HoldingData>): HoldingData {
        val matching = holdings.filter { it.from == holding.from && it.to == holding.to }
        val position = PositionCalculator.replay(matching).positionFor(holding.from, holding.to)
        return holding.copy(
            quantity = position?.quantity ?: zero,
            price = position?.averageCost ?: zero,
            date = matching.minOfOrNull { it.date } ?: holding.date,
            type = TradeType.BUY.name,
            fee = zero
        )
    }

    private fun dayPnl(position: Position, coins: List<Coin>): BigDecimal {
        val coin = coinFor(position.from, position.to, coins) ?: return zero
        val current = position.quantity.multiply(currentPrice(position.from, position.to, coins))
        val denominator = BigDecimal.ONE + coin.changePct24hRaw.toDecimal().divide(oneHundred, mathContext)
        if (denominator.signum() <= 0) return zero
        val previous = current.divide(denominator, mathContext)
        return current - previous
    }

    private fun changePercent(oldValue: BigDecimal, newValue: BigDecimal): BigDecimal {
        if (oldValue.signum() == 0) return zero
        return (newValue - oldValue).multiply(oneHundred).divide(oldValue, mathContext)
    }

    private fun coinFor(from: String, to: String, coins: List<Coin>): Coin? =
        coins.find { it.from == from && it.to == to }
            ?: coins.find { it.from == from }

    private fun currentPrice(from: String, to: String, coins: List<Coin>): BigDecimal =
        coinFor(from, to, coins)?.priceRaw?.toDecimal() ?: zero

    private fun Float.toDecimal(): BigDecimal = BigDecimal.valueOf(toDouble())
}
