package app.khom.pavlo.crypto.widget

import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.USD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class InvestmentsWidgetModelTest {

    @Test
    fun `content aggregates transactions and sorts positions by current value`() {
        val holdings = listOf(
            holding("BTC", "0.1", "20000", "Bitcoin"),
            holding("btc", "0.2", "25000", "Bitcoin"),
            holding("ETH", "1", "1000", "Ethereum")
        )
        val content = investmentsWidgetContent(
            holdings,
            listOf(
                Coin(from = "BTC", to = USD, priceRaw = 30000f),
                Coin(from = "ETH", to = USD, priceRaw = 1500f)
            )
        )

        assertDecimal("8000", content.investedValue)
        assertDecimal("10500", content.currentValue)
        assertDecimal("2500", content.totalPnl)
        assertDecimal("31.25", content.totalPnlPercent)
        assertEquals(listOf("BTC", "ETH"), content.positions.map { it.symbol })
        assertDecimal("0.3", content.positions.first().quantity)
        assertDecimal("9000", content.positions.first().currentValue)
    }

    @Test
    fun `missing market price keeps invested data but hides incomplete totals`() {
        val content = investmentsWidgetContent(
            listOf(
                holding("BTC", "0.1", "20000", "Bitcoin"),
                holding("SOL", "5", "100", "Solana")
            ),
            listOf(Coin(from = "BTC", to = USD, priceRaw = 30000f))
        )

        assertDecimal("2500", content.investedValue)
        assertNull(content.currentValue)
        assertNull(content.totalPnl)
        assertNull(content.totalPnlPercent)
        assertDecimal("80", content.positions.first { it.symbol == "BTC" }.allocationPercent)
        assertDecimal("20", content.positions.first { it.symbol == "SOL" }.allocationPercent)
    }

    @Test
    fun `first price wins so fresh prices take precedence over cached values`() {
        val content = investmentsWidgetContent(
            listOf(holding("SOL", "2", "100", "Solana")),
            listOf(
                Coin(from = "SOL", to = USD, priceRaw = 150f),
                Coin(from = "SOL", to = USD, priceRaw = 90f)
            )
        )

        assertDecimal("300", content.currentValue)
        assertDecimal("100", content.totalPnl)
    }

    private fun holding(
        from: String,
        quantity: String,
        price: String,
        name: String
    ) = HoldingData(
        from = from,
        to = USD,
        quantity = quantity.toBigDecimal(),
        price = price.toBigDecimal(),
        date = 1L,
        coinName = name
    )

    private fun assertDecimal(expected: String, actual: BigDecimal?) {
        requireNotNull(actual)
        val delta = actual.subtract(expected.toBigDecimal()).abs()
        assertTrue("Expected $expected, actual $actual", delta <= BigDecimal("0.000000001"))
    }
}