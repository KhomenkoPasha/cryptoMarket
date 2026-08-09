package app.khom.pavlo.crypto.widget

import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.HoldingData
import java.math.BigDecimal
import java.math.MathContext
import java.util.Locale

internal data class InvestmentsWidgetContent(
    val investedValue: BigDecimal,
    val currentValue: BigDecimal?,
    val totalPnl: BigDecimal?,
    val totalPnlPercent: BigDecimal?,
    val positions: List<InvestmentsWidgetPosition>
) {
    val isEmpty: Boolean get() = positions.isEmpty()
}

internal data class InvestmentsWidgetPosition(
    val symbol: String,
    val name: String,
    val quantity: BigDecimal,
    val currentValue: BigDecimal?,
    val allocationPercent: BigDecimal
)

internal fun investmentsWidgetContent(
    holdings: List<HoldingData>,
    coins: List<Coin>
): InvestmentsWidgetContent {
    val priceByPair = linkedMapOf<Pair<String, String>, BigDecimal>()
    coins.forEach { coin ->
        val price = coin.priceRaw
            .takeIf { it.isFinite() && it > 0f }
            ?.let { BigDecimal.valueOf(it.toDouble()) }
            ?: return@forEach
        val pair = coin.normalizedPair()
        if (!priceByPair.containsKey(pair)) {
            priceByPair[pair] = price
        }
    }

    val positions = holdings
        .groupBy(HoldingData::normalizedPair)
        .map { (pair, transactions) ->
            val quantity = transactions.sumOf(HoldingData::quantity)
            val invested = transactions.sumOf { it.quantity.multiply(it.price) }
            val current = priceByPair[pair]?.multiply(quantity)
            PositionDraft(
                symbol = pair.first,
                name = transactions.firstNotNullOfOrNull {
                    it.coinName.trim().takeIf(String::isNotEmpty)
                } ?: pair.first,
                quantity = quantity,
                investedValue = invested,
                currentValue = current
            )
        }
        .sortedByDescending { it.currentValue ?: it.investedValue }

    val investedValue = positions.sumOf(PositionDraft::investedValue)
    val hasAllPrices = positions.all { it.currentValue != null }
    val currentValue = if (positions.isNotEmpty() && hasAllPrices) {
        positions.sumOf { it.currentValue ?: BigDecimal.ZERO }
    } else {
        null
    }
    val allocationBase = currentValue
        ?.takeIf { it.signum() > 0 }
        ?: investedValue.takeIf { it.signum() > 0 }

    val visiblePositions = positions.map { position ->
        val allocationValue = if (currentValue != null) {
            position.currentValue ?: BigDecimal.ZERO
        } else {
            position.investedValue
        }
        InvestmentsWidgetPosition(
            symbol = position.symbol,
            name = position.name,
            quantity = position.quantity,
            currentValue = position.currentValue,
            allocationPercent = if (allocationBase != null) {
                allocationValue
                    .multiply(ONE_HUNDRED)
                    .divide(allocationBase, MathContext.DECIMAL128)
            } else {
                BigDecimal.ZERO
            }
        )
    }

    val totalPnl = currentValue?.subtract(investedValue)
    val totalPnlPercent = totalPnl?.let { pnl ->
        if (investedValue.signum() == 0) BigDecimal.ZERO
        else pnl.multiply(ONE_HUNDRED).divide(investedValue, MathContext.DECIMAL128)
    }
    return InvestmentsWidgetContent(
        investedValue = investedValue,
        currentValue = currentValue,
        totalPnl = totalPnl,
        totalPnlPercent = totalPnlPercent,
        positions = visiblePositions
    )
}

private data class PositionDraft(
    val symbol: String,
    val name: String,
    val quantity: BigDecimal,
    val investedValue: BigDecimal,
    val currentValue: BigDecimal?
)

private fun HoldingData.normalizedPair(): Pair<String, String> =
    from.trim().uppercase(Locale.US) to to.trim().uppercase(Locale.US)

private fun Coin.normalizedPair(): Pair<String, String> =
    from.trim().uppercase(Locale.US) to to.trim().uppercase(Locale.US)

private val ONE_HUNDRED = BigDecimal("100")