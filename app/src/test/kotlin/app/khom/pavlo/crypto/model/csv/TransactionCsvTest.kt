package app.khom.pavlo.crypto.model.csv

import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.TradeType
import app.khom.pavlo.crypto.model.USD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class TransactionCsvTest {

    private fun assertDecimal(expected: String, actual: BigDecimal) =
        assertEquals("Expected $expected, actual $actual", 0, expected.toBigDecimal().compareTo(actual))

    private fun holding(
        type: TradeType,
        quantity: String,
        price: String,
        fee: String = "0",
        date: Long = 1_700_000_000_000L,
        id: Long = 1,
        exchange: String = "",
        portfolio: Long = 1
    ) = HoldingData(
        id = id, from = "BTC", to = USD, quantity = quantity.toBigDecimal(), price = price.toBigDecimal(),
        date = date, coinName = "Bitcoin", exchange = exchange, type = type.name, fee = fee.toBigDecimal(),
        portfolioId = portfolio
    )

    // ---- parser ----

    @Test
    fun `parser handles quotes commas and embedded line breaks`() {
        val rows = CsvParser.parse("a,b,c\r\n\"x, y\",\"he said \"\"hi\"\"\",\"line1\nline2\"\r\n\r\n1,2,3")

        assertEquals(3, rows.size)
        assertEquals(listOf("x, y", "he said \"hi\"", "line1\nline2"), rows[1])
        assertEquals(listOf("1", "2", "3"), rows[2])
    }

    @Test
    fun `parser guesses semicolons and strips a byte order mark`() {
        val rows = CsvParser.parse(CsvParser.BYTE_ORDER_MARK + "Date;Coin\n2020-01-01;BTC")

        assertEquals(listOf("Date", "Coin"), rows[0])
        assertEquals(listOf("2020-01-01", "BTC"), rows[1])
    }

    @Test
    fun `writer quotes only what needs it and defuses spreadsheet formulas`() {
        assertEquals("a,\"b,c\",\"d\"\"e\"", CsvParser.line("a", "b,c", "d\"e"))
        assertEquals("'=SUM(A1)", CsvParser.safeText("=SUM(A1)"))
        assertEquals("Binance", CsvParser.safeText("Binance"))
    }

    // ---- native format ----

    @Test
    fun `native export imports back to the same transactions`() {
        val holdings = listOf(
            holding(TradeType.BUY, "0.12345678", "20000.5", fee = "3.25", id = 1, exchange = "Binance, spot"),
            holding(TradeType.SELL, "0.1", "30000", id = 2, date = 1_710_000_000_000L),
            holding(TradeType.TRANSFER_IN, "2", "0", id = 3, date = 1_720_000_000_000L)
        )

        val csv = TransactionCsv.exportNative(holdings) { "Main" }
        val preview = TransactionCsv.parse(csv)!!

        assertEquals(CsvSource.INVESTPULSE, preview.source)
        assertEquals(0, preview.skippedRows)
        assertEquals(3, preview.transactions.size)
        val first = preview.transactions[0]
        assertEquals("BTC", first.symbol)
        assertEquals(TradeType.BUY, first.type)
        assertDecimal("0.12345678", first.quantity)
        assertDecimal("20000.5", first.priceUsd)
        assertDecimal("3.25", first.feeUsd)
        assertEquals(1_700_000_000_000L, first.dateMillis)
        assertEquals("Binance, spot", first.exchange)
        assertEquals(TradeType.SELL, preview.transactions[1].type)
        assertEquals(TradeType.TRANSFER_IN, preview.transactions[2].type)
    }

    @Test
    fun `native rows that cannot be read are counted as skipped`() {
        val csv = TransactionCsv.NATIVE_HEADER + "\n" +
            "2024-01-01T00:00:00Z,BUY,BTC,Bitcoin,1,100,0,,\n" +
            "not a date,BUY,BTC,Bitcoin,1,100,0,,\n" +
            "2024-01-01T00:00:00Z,SWAP,BTC,Bitcoin,1,100,0,,\n" +
            "2024-01-01T00:00:00Z,BUY,BTC,Bitcoin,-5,100,0,,\n"

        val preview = TransactionCsv.parse(csv)!!

        assertEquals(1, preview.transactions.size)
        assertEquals(3, preview.skippedRows)
    }

    // ---- Koinly export ----

    @Test
    fun `koinly export maps buys and sales to sent and received legs`() {
        val csv = TransactionCsv.exportKoinly(
            listOf(
                holding(TradeType.BUY, "0.5", "20000", fee = "5", id = 1, exchange = "Binance"),
                holding(TradeType.SELL, "0.25", "40000", id = 2, date = 1_710_000_000_000L),
                holding(TradeType.TRANSFER_IN, "1", "0", id = 3, date = 1_720_000_000_000L)
            )
        )
        val rows = CsvParser.parse(csv)

        assertEquals(TransactionCsv.KOINLY_HEADER, rows[0].joinToString(","))
        // Buy: USD sent, BTC received, fee in USD.
        assertEquals("2023-11-14 22:13:20 UTC", rows[1][0])
        assertEquals(listOf("10000", "USD", "0.5", "BTC", "5", "USD"), rows[1].subList(1, 7))
        assertEquals("Binance", rows[1][10])
        // Sale: BTC sent, USD received.
        assertEquals(listOf("0.25", "BTC", "10000", "USD"), rows[2].subList(1, 5))
        // A deposit has only a received leg and no worth when the cost is unknown.
        assertEquals(listOf("", "", "1", "BTC"), rows[3].subList(1, 5))
        assertEquals("", rows[3][7])
    }

    // ---- Binance ----

    private val binanceSample = """
        Date(UTC),Pair,Side,Price,Executed,Amount,Fee
        2024-03-01 10:15:30,BTCUSDT,BUY,60000,0.01BTC,600USDT,0.6USDT
        2024-03-02 11:00:00,ETHUSDT,SELL,3000,2ETH,6000USDT,0.002ETH
        2024-03-03 12:00:00,BTCEUR,BUY,55000,0.01BTC,550EUR,0.55EUR
        2024-03-04 13:00:00,SOLUSDT,BUY,100,1SOL,100USDT,0.001BNB
        2024-03-05 14:00:00,ADAUSDT,HOLD,1,1ADA,1USDT,0USDT
    """.trimIndent()

    @Test
    fun `binance trades read units and convert fees to dollars`() {
        val preview = TransactionCsv.parse(binanceSample)!!

        assertEquals(CsvSource.BINANCE_TRADES, preview.source)
        // The euro pair and the unknown side are skipped.
        assertEquals(2 + 1, preview.transactions.size)
        assertEquals(2, preview.skippedRows)

        val btc = preview.transactions[0]
        assertEquals("BTC", btc.symbol)
        assertEquals(TradeType.BUY, btc.type)
        assertDecimal("0.01", btc.quantity)
        assertDecimal("60000", btc.priceUsd)
        assertDecimal("0.6", btc.feeUsd)
        assertEquals("Binance", btc.exchange)
        assertEquals(1_709_288_130_000L, btc.dateMillis)

        val eth = preview.transactions[1]
        assertEquals(TradeType.SELL, eth.type)
        // A fee in the coin itself is valued at the trade price: 0.002 ETH * 3000.
        assertDecimal("6", eth.feeUsd)

        // A fee in another asset (BNB) can't be priced and is left out.
        assertDecimal("0", preview.transactions[2].feeUsd)
    }

    // ---- Coinbase ----

    private val coinbaseNew = """
        Transactions
        User,Jane Doe,jane@example.com
        ID,Timestamp,Transaction Type,Asset,Quantity Transacted,Price Currency,Price at Transaction,Subtotal,Total (inclusive of fees and/or spread),Fees and/or Spread,Notes
        1,2024-02-01T09:00:00Z,Buy,BTC,0.1,USD,${'$'}40000.00,${'$'}4000.00,${'$'}4040.00,${'$'}40.00,Bought 0.1 BTC
        2,2024-02-02T09:00:00Z,Sell,ETH,2,USD,${'$'}2500.00,${'$'}5000.00,${'$'}4950.00,${'$'}50.00,Sold
        3,2024-02-03T09:00:00Z,Receive,SOL,10,USD,${'$'}100.00,${'$'}1000.00,${'$'}1000.00,${'$'}0.00,Received
        4,2024-02-04T09:00:00Z,Send,SOL,4,USD,${'$'}110.00,${'$'}440.00,${'$'}440.00,${'$'}0.00,Sent
        5,2024-02-05T09:00:00Z,Deposit,USD,1000,USD,${'$'}1.00,${'$'}1000.00,${'$'}1000.00,${'$'}0.00,Fiat in
        6,2024-02-06T09:00:00Z,Buy,BTC,0.1,EUR,E40000.00,E4000.00,E4040.00,E40.00,Euro buy
    """.trimIndent()

    @Test
    fun `coinbase report is found below its preamble and fiat rows are ignored`() {
        val preview = TransactionCsv.parse(coinbaseNew)!!

        assertEquals(CsvSource.COINBASE, preview.source)
        assertEquals(4, preview.transactions.size)
        // The euro purchase can't be valued in dollars; the fiat deposit is simply not a coin transaction.
        assertEquals(1, preview.skippedRows)

        val buy = preview.transactions[0]
        assertEquals(TradeType.BUY, buy.type)
        assertDecimal("40000", buy.priceUsd)
        assertDecimal("40", buy.feeUsd)
        assertEquals("Coinbase", buy.exchange)

        assertEquals(TradeType.SELL, preview.transactions[1].type)
        assertDecimal("50", preview.transactions[1].feeUsd)
        assertEquals(TradeType.TRANSFER_IN, preview.transactions[2].type)
        assertEquals(TradeType.TRANSFER_OUT, preview.transactions[3].type)
        // Transfers carry no fee in the app.
        assertDecimal("0", preview.transactions[2].feeUsd)
    }

    @Test
    fun `older coinbase layout with usd columns is understood`() {
        val csv = """
            Timestamp,Transaction Type,Asset,Quantity Transacted,USD Spot Price at Transaction,USD Subtotal,USD Total (inclusive of fees),USD Fees,Notes
            2021-05-01T12:00:00Z,Buy,BTC,0.5,"${'$'}55,000.00","${'$'}27,500.00","${'$'}27,540.00","${'$'}40.00",Bought
        """.trimIndent()

        val preview = TransactionCsv.parse(csv)!!

        assertEquals(1, preview.transactions.size)
        assertDecimal("55000", preview.transactions[0].priceUsd)
        assertDecimal("40", preview.transactions[0].feeUsd)
    }

    // ---- detection and helpers ----

    @Test
    fun `unknown files are not recognized`() {
        assertNull(TransactionCsv.parse("name,age\nBob,3"))
        assertNull(TransactionCsv.parse(""))
    }

    @Test
    fun `amounts with units and currency signs are parsed`() {
        assertDecimal("1234.5", TransactionCsv.parseDecimal("\$1,234.50")!!)
        assertNull(TransactionCsv.parseDecimal("n/a"))
        val withUnit = TransactionCsv.parseWithUnit("30,000 usdt")!!
        assertDecimal("30000", withUnit.first)
        assertEquals("USDT", withUnit.second)
        assertNull(TransactionCsv.parseWithUnit("abc"))
    }

    @Test
    fun `dates are read as utc in the usual exchange formats`() {
        assertEquals(1_709_288_130_000L, TransactionCsv.parseDate("2024-03-01 10:15:30"))
        assertEquals(1_709_288_130_000L, TransactionCsv.parseDate("2024-03-01T10:15:30Z"))
        assertEquals(1_709_288_130_000L, TransactionCsv.parseDate("2024-03-01 10:15:30 UTC"))
        assertNotNull(TransactionCsv.parseDate("2024-03-01"))
        assertNull(TransactionCsv.parseDate("yesterday"))
        assertTrue(TransactionCsv.parseDate("") == null)
    }
}
