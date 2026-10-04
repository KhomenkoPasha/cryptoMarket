package app.khom.pavlo.crypto.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PortfolioHistoryTest {

    private val dayMillis = PortfolioHistoryBuilder.DAY_MILLIS

    private fun tx(
        type: TradeType,
        quantity: String,
        price: String,
        day: Long,
        from: String = "BTC",
        fee: String = "0",
        id: Long = day
    ) = HoldingData(
        id = id,
        from = from,
        to = USD,
        quantity = quantity.toBigDecimal(),
        price = price.toBigDecimal(),
        date = day * dayMillis + dayMillis / 2,
        type = type.name,
        fee = fee.toBigDecimal()
    )

    private fun assertDecimal(expected: String, actual: BigDecimal) =
        assertEquals("Expected $expected, actual $actual", 0, expected.toBigDecimal().compareTo(actual))

    @Test
    fun `values follow the closing prices day by day`() {
        val buy = tx(TradeType.BUY, "1", "100", day = 10)
        val prices = mapOf("BTC" to mapOf(10L to 100.0, 11L to 110.0, 12L to 120.0))

        val snapshots = PortfolioHistoryBuilder.build(1L, listOf(buy), prices, fromDay = 0, toDay = 13)

        // History starts with the first transaction and a missing day carries the last close forward.
        assertEquals(listOf(10L, 11L, 12L, 13L), snapshots.map { it.day })
        assertDecimal("100", snapshots[0].value)
        assertDecimal("110", snapshots[1].value)
        assertDecimal("120", snapshots[2].value)
        assertDecimal("120", snapshots[3].value)
        assertDecimal("100", snapshots[3].invested)
    }

    @Test
    fun `a buy is money in and a sale is money out on the day it happens`() {
        val holdings = listOf(
            tx(TradeType.BUY, "1", "100", day = 10, fee = "2"),
            tx(TradeType.SELL, "0.5", "120", day = 12, fee = "1")
        )
        val prices = mapOf("BTC" to mapOf(10L to 100.0, 11L to 110.0, 12L to 120.0))

        val snapshots = PortfolioHistoryBuilder.build(1L, holdings, prices, 10, 12)

        assertDecimal("102", snapshots[0].netFlow)
        assertDecimal("0", snapshots[1].netFlow)
        assertDecimal("-59", snapshots[2].netFlow)
        // Half the coin is left, valued at that day's close.
        assertDecimal("60", snapshots[2].value)
    }

    @Test
    fun `days before the first known price use that first price`() {
        val buy = tx(TradeType.BUY, "2", "50", day = 5)
        val prices = mapOf("BTC" to mapOf(8L to 70.0))

        val snapshots = PortfolioHistoryBuilder.build(1L, listOf(buy), prices, 5, 8)

        assertDecimal("140", snapshots.first().value)
    }

    @Test
    fun `a coin without any price falls back to its cost`() {
        val buy = tx(TradeType.BUY, "2", "50", day = 5)

        val snapshots = PortfolioHistoryBuilder.build(1L, listOf(buy), emptyMap(), 5, 6)

        assertDecimal("100", snapshots.last().value)
    }

    @Test
    fun `the start of the range is clipped but never moved before the first transaction`() {
        val buy = tx(TradeType.BUY, "1", "10", day = 20)
        val prices = mapOf("BTC" to mapOf(20L to 10.0))

        assertEquals(20L, PortfolioHistoryBuilder.build(1L, listOf(buy), prices, 0, 21).first().day)
        assertEquals(21L, PortfolioHistoryBuilder.build(1L, listOf(buy), prices, 21, 21).single().day)
        assertTrue(PortfolioHistoryBuilder.build(1L, emptyList(), prices, 0, 5).isEmpty())
    }

    @Test
    fun `transfers are valued at the market price on their day`() {
        val receive = tx(TradeType.TRANSFER_IN, "1", "0", day = 10)
        val prices = mapOf("BTC" to mapOf(10L to 300.0))

        val snapshot = PortfolioHistoryBuilder.build(1L, listOf(receive), prices, 10, 10).single()

        assertDecimal("300", snapshot.netFlow)
        assertDecimal("300", snapshot.value)
    }

    private fun point(day: Long, value: String, flow: String = "0") =
        PerformancePoint(day, value.toBigDecimal(), flow.toBigDecimal())

    @Test
    fun `performance ignores deposits and finds drawdown best and worst day`() {
        val stats = PortfolioPerformance.analyze(
            listOf(
                point(1, "100", flow = "100"),
                point(2, "110"),
                point(3, "99"),
                // 50 was added, so 150 is only +1.01% over the 99 before it.
                point(4, "150", flow = "50")
            )
        )

        assertEquals(10.0, stats.maxDrawdownPercent, 1e-9)
        assertEquals(2L, stats.bestDay!!.day)
        assertEquals(10.0, stats.bestDay!!.percent, 1e-9)
        assertEquals(3L, stats.worstDay!!.day)
        assertEquals(-10.0, stats.worstDay!!.percent, 1e-9)
        assertEquals(0.0, stats.periodReturnPercent!!, 1e-9)
    }

    @Test
    fun `withdrawals are not counted as losses`() {
        val stats = PortfolioPerformance.analyze(
            listOf(point(1, "200"), point(2, "100", flow = "-100"))
        )

        assertEquals(0.0, stats.maxDrawdownPercent, 1e-9)
        assertEquals(0.0, stats.worstDay!!.percent, 1e-9)
    }

    @Test
    fun `a steadily growing portfolio has no drawdown`() {
        val stats = PortfolioPerformance.analyze(
            listOf(point(1, "100"), point(2, "101"), point(3, "103"))
        )

        assertEquals(0.0, stats.maxDrawdownPercent, 1e-9)
        assertNotNull(stats.bestDay)
        assertEquals(3.0, stats.periodReturnPercent!!, 1e-9)
    }

    @Test
    fun `too little data produces no returns`() {
        assertNull(PortfolioPerformance.analyze(emptyList()).bestDay)
        assertNull(PortfolioPerformance.analyze(listOf(point(1, "100"))).periodReturnPercent)
        // A day that starts from nothing has no return to measure.
        assertNull(PortfolioPerformance.analyze(listOf(point(1, "0"), point(2, "50", flow = "50"))).worstDay)
    }
}
