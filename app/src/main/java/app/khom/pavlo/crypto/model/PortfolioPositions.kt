package app.khom.pavlo.crypto.model

import java.math.BigDecimal
import java.math.MathContext

/** What a transaction does to a holding. */
enum class TradeType {
    /** Coins bought with fiat; the fee is added to the cost basis. */
    BUY,

    /** Coins sold for fiat; the fee reduces the proceeds and the sale realizes a profit or loss. */
    SELL,

    /** Coins received from outside (wallet transfer, airdrop). The price is the cost basis per unit. */
    TRANSFER_IN,

    /** Coins sent out. They leave at their average cost, so no profit or loss is realized. */
    TRANSFER_OUT;

    /** True for types that add coins to the position. */
    val isAcquisition: Boolean get() = this == BUY || this == TRANSFER_IN

    companion object {
        fun fromName(name: String?): TradeType = values().firstOrNull { it.name == name } ?: BUY
    }
}

/** The state of one coin after replaying all of its transactions. */
data class Position(
    val portfolioId: Long,
    val from: String,
    val to: String,
    /** Quantity currently held. */
    val quantity: BigDecimal,
    /** What the held quantity cost, buy fees included. */
    val costBasis: BigDecimal,
    /** Profit or loss already locked in by sales. */
    val realizedPnl: BigDecimal,
    /** Cost basis of everything that was sold. */
    val soldCostBasis: BigDecimal,
    /** Total fees paid on buys and sales. */
    val fees: BigDecimal,
    val firstDate: Long,
    /** True when a sale or transfer out asked for more than was held at that point. */
    val oversold: Boolean
) {
    val averageCost: BigDecimal
        get() = if (quantity.signum() > 0) costBasis.divide(quantity, MATH_CONTEXT) else BigDecimal.ZERO
}

/** The outcome of one sale, shown on its transaction card. */
data class SaleResult(
    val proceeds: BigDecimal,
    val costBasis: BigDecimal,
    val realizedPnl: BigDecimal
)

class PositionReport(
    val positions: List<Position>,
    /** Sale results keyed by transaction id. */
    val sales: Map<Long, SaleResult>
) {
    /** The coin across every portfolio in the report, merged into one position. */
    fun positionFor(from: String, to: String): Position? {
        val matching = positions.filter { it.from == from && it.to == to }
        if (matching.isEmpty()) return null
        if (matching.size == 1) return matching.first()
        return Position(
            portfolioId = ALL_PORTFOLIOS_ID,
            from = from,
            to = to,
            quantity = matching.sumOf { it.quantity },
            costBasis = matching.sumOf { it.costBasis },
            realizedPnl = matching.sumOf { it.realizedPnl },
            soldCostBasis = matching.sumOf { it.soldCostBasis },
            fees = matching.sumOf { it.fees },
            firstDate = matching.minOf { it.firstDate },
            oversold = matching.any { it.oversold }
        )
    }

    val hasOversell: Boolean get() = positions.any { it.oversold }
}

internal val MATH_CONTEXT: MathContext = MathContext.DECIMAL128

/**
 * Replays transactions in time order using the average cost method, separately for every
 * portfolio: the cost of every coin held is pooled, a sale removes coins at the pool's average cost, and the difference to the sale proceeds
 * is the realized profit.
 */
object PositionCalculator {

    fun replay(holdings: List<HoldingData>): PositionReport {
        val sales = HashMap<Long, SaleResult>()
        val positions = holdings
            .groupBy { Triple(it.portfolioId, it.from, it.to) }
            .map { (key, transactions) -> replayPair(key.first, key.second, key.third, transactions, sales) }
        return PositionReport(positions, sales)
    }

    private fun replayPair(
        portfolioId: Long,
        from: String,
        to: String,
        transactions: List<HoldingData>,
        sales: MutableMap<Long, SaleResult>
    ): Position {
        var quantity = BigDecimal.ZERO
        var cost = BigDecimal.ZERO
        var realized = BigDecimal.ZERO
        var soldCost = BigDecimal.ZERO
        var fees = BigDecimal.ZERO
        var oversold = false

        val ordered = transactions.sortedWith(
            compareBy<HoldingData> { it.date }
                .thenBy { if (it.tradeType.isAcquisition) 0 else 1 }
                .thenBy { it.id }
        )
        ordered.forEach { transaction ->
            val amount = transaction.quantity
            when (transaction.tradeType) {
                TradeType.BUY -> {
                    quantity += amount
                    cost += amount.multiply(transaction.price) + transaction.fee
                    fees += transaction.fee
                }
                TradeType.TRANSFER_IN -> {
                    quantity += amount
                    cost += amount.multiply(transaction.price)
                }
                TradeType.SELL, TradeType.TRANSFER_OUT -> {
                    val average = if (quantity.signum() > 0) cost.divide(quantity, MATH_CONTEXT) else BigDecimal.ZERO
                    val removedQuantity = amount.min(quantity.max(BigDecimal.ZERO))
                    // Selling the whole position removes exactly what is left, with no rounding residue.
                    val removedCost = if (removedQuantity.signum() > 0 && removedQuantity.compareTo(quantity) == 0) {
                        cost
                    } else {
                        average.multiply(removedQuantity)
                    }
                    if (amount > quantity) oversold = true
                    if (transaction.tradeType == TradeType.SELL) {
                        val proceeds = amount.multiply(transaction.price) - transaction.fee
                        val profit = proceeds - removedCost
                        realized += profit
                        soldCost += removedCost
                        fees += transaction.fee
                        sales[transaction.id] = SaleResult(proceeds, removedCost, profit)
                    }
                    quantity -= removedQuantity
                    cost -= removedCost
                }
            }
        }
        return Position(
            portfolioId = portfolioId,
            from = from,
            to = to,
            quantity = quantity,
            costBasis = cost.max(BigDecimal.ZERO),
            realizedPnl = realized,
            soldCostBasis = soldCost,
            fees = fees,
            firstDate = transactions.minOfOrNull { it.date } ?: 0L,
            oversold = oversold
        )
    }
}
