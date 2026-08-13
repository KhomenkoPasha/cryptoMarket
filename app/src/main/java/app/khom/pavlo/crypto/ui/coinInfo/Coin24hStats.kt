package app.khom.pavlo.crypto.ui.coinInfo

import app.khom.pavlo.crypto.model.HistoData

internal data class Coin24hStats(
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val change: Double,
    val changePercent: Double
)

internal fun calculateCoin24hStats(candles: List<HistoData>): Coin24hStats? {
    val validCandles = candles
        .asSequence()
        .filter(HistoData::hasUsablePrices)
        .sortedBy(HistoData::time)
        .toList()
    if (validCandles.isEmpty()) return null

    val open = validCandles.first().open.toDouble()
    val close = validCandles.last().close.toDouble()
    val change = close - open
    return Coin24hStats(
        open = open,
        high = validCandles.maxOf { it.high.toDouble() },
        low = validCandles.minOf { it.low.toDouble() },
        close = close,
        change = change,
        changePercent = change / open * 100.0
    )
}

private fun HistoData.hasUsablePrices(): Boolean =
    time > 0L &&
        open.isFinite() && open > 0f &&
        high.isFinite() && high > 0f &&
        low.isFinite() && low > 0f &&
        close.isFinite() && close > 0f