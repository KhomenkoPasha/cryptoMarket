package app.khom.pavlo.crypto.model.csv

import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.TradeType
import app.khom.pavlo.crypto.model.USD
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class CsvImportPlannerTest {

    private fun row(
        symbol: String = "BTC",
        type: TradeType = TradeType.BUY,
        quantity: String = "1",
        price: String = "100",
        date: Long = 1_700_000_000_000L,
        name: String = ""
    ) = ImportedTransaction(
        symbol = symbol, type = type, quantity = BigDecimal(quantity), priceUsd = BigDecimal(price),
        feeUsd = BigDecimal.ZERO, dateMillis = date, exchange = "Binance", coinName = name
    )

    private fun existing(
        symbol: String = "BTC",
        name: String = "Bitcoin",
        quantity: String = "1",
        price: String = "100",
        date: Long = 1_700_000_000_000L,
        portfolio: Long = 1
    ) = HoldingData(
        id = 1, from = symbol, to = USD, quantity = BigDecimal(quantity), price = BigDecimal(price), date = date,
        coinName = name, type = TradeType.BUY.name, portfolioId = portfolio
    )

    private fun plan(imported: List<ImportedTransaction>, existing: List<HoldingData> = emptyList(), portfolio: Long = 1) =
        CsvImportPlanner.plan(portfolio, imported, existing) { "id-$it" }

    @Test
    fun `new rows become transactions of the chosen portfolio`() {
        val result = plan(listOf(row(symbol = "eth", quantity = "2.50", price = "3000")), portfolio = 7)

        assertEquals(0, result.duplicates)
        val holding = result.fresh.single()
        assertEquals("ETH", holding.from)
        assertEquals(USD, holding.to)
        assertEquals(7L, holding.portfolioId)
        assertEquals("id-ETH", holding.coinId)
        assertEquals("BUY", holding.type)
        assertEquals("Binance", holding.exchange)
    }

    @Test
    fun `rows the portfolio already has are skipped even if numbers are written differently`() {
        val result = plan(
            imported = listOf(row(quantity = "1.0", price = "100.00"), row(quantity = "2")),
            existing = listOf(existing(quantity = "1", price = "100"))
        )

        assertEquals(1, result.duplicates)
        assertEquals(1, result.fresh.size)
        assertEquals(0, BigDecimal("2").compareTo(result.fresh.single().quantity))
    }

    @Test
    fun `the same row twice in one file is imported once`() {
        val result = plan(listOf(row(), row()))

        assertEquals(1, result.fresh.size)
        assertEquals(1, result.duplicates)
    }

    @Test
    fun `another portfolio's transactions do not count as duplicates`() {
        val result = plan(listOf(row()), existing = listOf(existing(portfolio = 2)), portfolio = 1)

        assertEquals(1, result.fresh.size)
        assertEquals(0, result.duplicates)
    }

    @Test
    fun `different type or time is not a duplicate`() {
        val result = plan(
            listOf(row(type = TradeType.SELL), row(date = 1_700_000_005_000L)),
            existing = listOf(existing())
        )

        assertEquals(2, result.fresh.size)
        assertEquals(0, result.duplicates)
    }

    @Test
    fun `coin names come from the file, else from an existing transaction, else the ticker`() {
        val result = plan(
            imported = listOf(
                row(symbol = "BTC", quantity = "3"),
                row(symbol = "ETH", quantity = "3", name = "Ether"),
                row(symbol = "SOL", quantity = "3")
            ),
            existing = listOf(existing(symbol = "BTC", name = "Bitcoin"))
        )

        assertEquals(listOf("Bitcoin", "Ether", "SOL"), result.fresh.map { it.coinName })
    }

    @Test
    fun `a name equal to the ticker does not hide a better one`() {
        val result = plan(
            imported = listOf(row(symbol = "BTC", name = "BTC", quantity = "5")),
            existing = listOf(existing(symbol = "BTC", name = "BTC"), existing(symbol = "BTC", name = "Bitcoin", quantity = "9"))
        )

        assertEquals("Bitcoin", result.fresh.single().coinName)
    }
}
