package app.khom.pavlo.crypto.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PortfolioCalculatorTest {

    @Test
    fun `summary keeps decimal precision for fractional holdings`() {
        val holdings = listOf(
            holding("BTC", quantity = "0.12345678", price = "20000.12345678"),
            holding("ETH", quantity = "2.5", price = "1000.25")
        )
        val coins = listOf(
            Coin(from = "BTC", to = USD, priceRaw = 30000.5f, changePct24hRaw = 10f),
            Coin(from = "ETH", to = USD, priceRaw = 1500.75f, changePct24hRaw = -20f)
        )

        val result = PortfolioCalculator.summary(holdings, coins)

        assertDecimal("4969.7758415765279684", result.investedValue)
        assertDecimal("7455.640128390", result.currentValue)
        assertDecimal("2485.8642868134720316", result.totalPnl)
        assertDecimal("50.019646", result.totalPnlPercent, tolerance = "0.000001")
    }

    @Test
    fun `stats aggregate every transaction for the same pair`() {
        val first = holding("BTC", quantity = "0.1", price = "20000", date = 1)
        val second = holding("BTC", quantity = "0.2", price = "25000", date = 2)
        val eth = holding("ETH", quantity = "1", price = "1000", date = 3)
        val holdings = listOf(first, second, eth)
        val coins = listOf(
            Coin(from = "BTC", to = USD, priceRaw = 30000f, changePct24hRaw = 5f),
            Coin(from = "ETH", to = USD, priceRaw = 1000f)
        )

        val stats = PortfolioCalculator.statsFor(first, holdings, coins)
        val aggregate = PortfolioCalculator.aggregatePair(first, holdings)

        assertDecimal("23333.33333333333333333333333333333", stats.averageBuyPrice)
        assertDecimal("90", stats.allocationPercent)
        assertDecimal("0.3", aggregate.quantity)
        assertEquals(1L, aggregate.date)
    }

    @Test
    fun `zero invested value produces zero percentage instead of division error`() {
        val freeHolding = holding("BTC", quantity = "1", price = "0")
        val result = PortfolioCalculator.summary(
            listOf(freeHolding),
            listOf(Coin(from = "BTC", to = USD, priceRaw = 100f))
        )

        assertDecimal("100", result.currentValue)
        assertDecimal("0", result.totalPnlPercent)
    }

    private fun holding(
        from: String,
        quantity: String,
        price: String,
        date: Long = 1
    ) = HoldingData(
        from = from,
        to = USD,
        quantity = quantity.toBigDecimal(),
        price = price.toBigDecimal(),
        date = date
    )

    private fun assertDecimal(
        expected: String,
        actual: BigDecimal,
        tolerance: String = "0.000000000001"
    ) {
        val delta = actual.subtract(expected.toBigDecimal()).abs()
        assertTrue("Expected $expected, actual $actual", delta <= tolerance.toBigDecimal())
    }
}
