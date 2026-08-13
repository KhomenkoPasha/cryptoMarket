package app.khom.pavlo.crypto.ui.coinInfo

import app.khom.pavlo.crypto.model.HistoData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Coin24hStatsTest {

    @Test
    fun `calculates 24 hour values from chronologically ordered candles`() {
        val stats = calculateCoin24hStats(
            listOf(
                candle(time = 3, open = 105f, high = 112f, low = 101f, close = 110f),
                candle(time = 1, open = 100f, high = 108f, low = 98f, close = 104f),
                candle(time = 2, open = 104f, high = 109f, low = 99f, close = 105f)
            )
        )!!

        assertEquals(100.0, stats.open, DELTA)
        assertEquals(112.0, stats.high, DELTA)
        assertEquals(98.0, stats.low, DELTA)
        assertEquals(110.0, stats.close, DELTA)
        assertEquals(10.0, stats.change, DELTA)
        assertEquals(10.0, stats.changePercent, DELTA)
    }

    @Test
    fun `ignores invalid candles`() {
        val stats = calculateCoin24hStats(
            listOf(
                candle(time = 1, open = 0f, high = 10f, low = 5f, close = 8f),
                candle(time = 2, open = 20f, high = 24f, low = 18f, close = 22f)
            )
        )!!

        assertEquals(20.0, stats.open, DELTA)
        assertEquals(22.0, stats.close, DELTA)
    }

    @Test
    fun `returns null when no usable candles exist`() {
        assertNull(calculateCoin24hStats(emptyList()))
        assertNull(
            calculateCoin24hStats(
                listOf(candle(time = 1, open = Float.NaN, high = 1f, low = 1f, close = 1f))
            )
        )
    }

    private fun candle(
        time: Long,
        open: Float,
        high: Float,
        low: Float,
        close: Float
    ): HistoData = HistoData(
        time = time,
        open = open,
        high = high,
        low = low,
        close = close,
        volumeFrom = 0f,
        volumeTo = 0f
    )

    private companion object {
        const val DELTA = 0.0001
    }
}