package app.khom.pavlo.crypto.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PositionCalculatorTest {

    private var nextId = 1L

    private fun tx(
        type: TradeType,
        quantity: String,
        price: String,
        fee: String = "0",
        date: Long = nextId,
        from: String = "BTC",
        portfolioId: Long = DEFAULT_PORTFOLIO_ID
    ) = HoldingData(
        id = nextId++,
        from = from,
        to = USD,
        quantity = quantity.toBigDecimal(),
        price = price.toBigDecimal(),
        date = date,
        type = type.name,
        fee = fee.toBigDecimal(),
        portfolioId = portfolioId
    )

    private fun assertDecimal(expected: String, actual: BigDecimal) =
        assertEquals("Expected $expected, actual $actual", 0, expected.toBigDecimal().compareTo(actual))

    @Test
    fun `buy fees are added to the cost basis`() {
        val position = PositionCalculator.replay(listOf(tx(TradeType.BUY, "2", "100", fee = "5")))
            .positions.single()

        assertDecimal("2", position.quantity)
        assertDecimal("205", position.costBasis)
        assertDecimal("102.5", position.averageCost)
        assertDecimal("5", position.fees)
    }

    @Test
    fun `a sale realizes profit against the average cost`() {
        val holdings = listOf(
            tx(TradeType.BUY, "1", "100", fee = "1"),
            tx(TradeType.BUY, "1", "200"),
            tx(TradeType.SELL, "1", "300", fee = "2")
        )

        val report = PositionCalculator.replay(holdings)
        val position = report.positions.single()
        val sale = report.sales.getValue(holdings.last().id)

        // Average cost is (101 + 200) / 2 = 150.5; proceeds are 300 - 2 = 298.
        assertDecimal("298", sale.proceeds)
        assertDecimal("150.5", sale.costBasis)
        assertDecimal("147.5", sale.realizedPnl)
        assertDecimal("1", position.quantity)
        assertDecimal("150.5", position.costBasis)
        assertDecimal("147.5", position.realizedPnl)
        assertDecimal("150.5", position.soldCostBasis)
        assertDecimal("3", position.fees)
        assertFalse(position.oversold)
    }

    @Test
    fun `a sale at a loss realizes a negative result`() {
        val holdings = listOf(tx(TradeType.BUY, "2", "100"), tx(TradeType.SELL, "1", "60"))

        val sale = PositionCalculator.replay(holdings).sales.getValue(holdings.last().id)

        assertDecimal("-40", sale.realizedPnl)
    }

    @Test
    fun `selling everything leaves no quantity and no cost`() {
        val holdings = listOf(
            tx(TradeType.BUY, "0.3", "33333.333333"),
            tx(TradeType.BUY, "0.7", "41000.123456"),
            tx(TradeType.SELL, "1", "50000")
        )

        val position = PositionCalculator.replay(holdings).positions.single()

        assertDecimal("0", position.quantity)
        assertDecimal("0", position.costBasis)
        assertDecimal("0", position.averageCost)
    }

    @Test
    fun `selling more than is held is clamped and flagged`() {
        val holdings = listOf(tx(TradeType.BUY, "1", "100"), tx(TradeType.SELL, "3", "200"))

        val report = PositionCalculator.replay(holdings)

        assertTrue(report.hasOversell)
        assertDecimal("0", report.positions.single().quantity)
        // Only the one coin that was held has a cost to set against the proceeds.
        assertDecimal("500", report.sales.getValue(holdings.last().id).realizedPnl)
    }

    @Test
    fun `transfers move coins without realizing profit`() {
        val holdings = listOf(
            tx(TradeType.BUY, "2", "100"),
            tx(TradeType.TRANSFER_OUT, "1", "0"),
            tx(TradeType.TRANSFER_IN, "1", "300")
        )

        val position = PositionCalculator.replay(holdings).positions.single()

        // 200 cost, minus 100 leaving, plus 300 arriving at its stated cost basis.
        assertDecimal("2", position.quantity)
        assertDecimal("400", position.costBasis)
        assertDecimal("0", position.realizedPnl)
    }

    @Test
    fun `transactions are replayed in date order whatever the input order`() {
        val sell = tx(TradeType.SELL, "1", "150", date = 20)
        val buy = tx(TradeType.BUY, "1", "100", date = 10)

        val report = PositionCalculator.replay(listOf(sell, buy))

        assertFalse(report.hasOversell)
        assertDecimal("50", report.sales.getValue(sell.id).realizedPnl)
    }

    @Test
    fun `a buy and sale on the same timestamp are replayed buy first`() {
        val sell = tx(TradeType.SELL, "1", "150", date = 5)
        val buy = tx(TradeType.BUY, "1", "100", date = 5)

        val report = PositionCalculator.replay(listOf(sell, buy))

        assertFalse(report.hasOversell)
        assertDecimal("50", report.sales.getValue(sell.id).realizedPnl)
    }

    @Test
    fun `coins are tracked independently`() {
        val holdings = listOf(
            tx(TradeType.BUY, "1", "100", from = "BTC"),
            tx(TradeType.BUY, "10", "5", from = "ETH"),
            tx(TradeType.SELL, "5", "7", from = "ETH")
        )

        val report = PositionCalculator.replay(holdings)

        assertDecimal("1", report.positionFor("BTC", USD)!!.quantity)
        assertDecimal("5", report.positionFor("ETH", USD)!!.quantity)
        assertDecimal("10", report.positionFor("ETH", USD)!!.realizedPnl)
        assertDecimal("0", report.positionFor("BTC", USD)!!.realizedPnl)
    }

    @Test
    fun `portfolios are replayed independently`() {
        val holdings = listOf(
            tx(TradeType.BUY, "1", "100", portfolioId = 1),
            tx(TradeType.BUY, "1", "200", portfolioId = 2),
            tx(TradeType.SELL, "1", "300", portfolioId = 2)
        )

        val report = PositionCalculator.replay(holdings)

        // The sale only sees the 200 paid in its own portfolio, not a pooled 150 average.
        assertDecimal("100", report.sales.getValue(holdings.last().id).realizedPnl)
        assertFalse(report.hasOversell)
    }

    @Test
    fun `selling in one portfolio never uses coins held in another`() {
        val holdings = listOf(
            tx(TradeType.BUY, "1", "100", portfolioId = 1),
            tx(TradeType.SELL, "1", "150", portfolioId = 2)
        )

        assertTrue(PositionCalculator.replay(holdings).hasOversell)
    }

    @Test
    fun `the merged position adds up every portfolio`() {
        val holdings = listOf(
            tx(TradeType.BUY, "1", "100", portfolioId = 1),
            tx(TradeType.BUY, "2", "250", portfolioId = 2),
            tx(TradeType.SELL, "1", "300", portfolioId = 2)
        )

        val merged = PositionCalculator.replay(holdings).positionFor("BTC", USD)!!

        assertDecimal("2", merged.quantity)
        assertDecimal("350", merged.costBasis)
        assertDecimal("50", merged.realizedPnl)
    }

    @Test
    fun `unknown stored types are treated as buys`() {
        assertEquals(TradeType.BUY, TradeType.fromName("SOMETHING"))
        assertEquals(TradeType.SELL, TradeType.fromName("SELL"))
    }

    @Test
    fun `summary separates realized from unrealized profit`() {
        val holdings = listOf(
            tx(TradeType.BUY, "2", "100", fee = "4"),
            tx(TradeType.SELL, "1", "180", fee = "2")
        )
        val coins = listOf(Coin(from = "BTC", to = USD, priceRaw = 150f))

        val summary = PortfolioCalculator.summary(holdings, coins)

        // Cost 204 -> avg 102. Sale: 178 - 102 = 76 realized. Held: 1 coin, cost 102, worth 150.
        assertDecimal("102", summary.investedValue)
        assertDecimal("150", summary.currentValue)
        assertDecimal("48", summary.unrealizedPnl)
        assertDecimal("76", summary.realizedPnl)
        assertDecimal("124", summary.totalPnl)
        assertDecimal("6", summary.feesPaid)
        assertTrue(summary.hasSales)
        // 124 profit on a cost base of 102 held + 102 sold.
        assertEquals(0, "60.7843137".toBigDecimal().compareTo(summary.totalPnlPercent.setScale(7, java.math.RoundingMode.HALF_UP)))
    }

    @Test
    fun `summary without sales matches the plain purchase math`() {
        val holdings = listOf(tx(TradeType.BUY, "2", "100"))
        val summary = PortfolioCalculator.summary(holdings, listOf(Coin(from = "BTC", to = USD, priceRaw = 120f)))

        assertDecimal("200", summary.investedValue)
        assertDecimal("40", summary.totalPnl)
        assertDecimal("20", summary.totalPnlPercent)
        assertFalse(summary.hasSales)
    }

    @Test
    fun `aggregate keeps the net quantity and average cost after a sale`() {
        val holdings = listOf(tx(TradeType.BUY, "2", "100"), tx(TradeType.SELL, "0.5", "300"))

        val aggregate = PortfolioCalculator.aggregatePair(holdings.first(), holdings)

        assertDecimal("1.5", aggregate.quantity)
        assertDecimal("100", aggregate.price)
    }

    @Test
    fun `a fully sold coin aggregates to zero quantity`() {
        val holdings = listOf(tx(TradeType.BUY, "1", "100"), tx(TradeType.SELL, "1", "200"))

        assertDecimal("0", PortfolioCalculator.aggregatePair(holdings.first(), holdings).quantity)
    }

    @Test
    fun `transaction stats for a buy include the fee`() {
        val buy = tx(TradeType.BUY, "1", "100", fee = "10")

        val stats = PortfolioCalculator.transactionStats(buy, listOf(Coin(from = "BTC", to = USD, priceRaw = 130f)))

        assertDecimal("110", stats.totalSpent)
        assertDecimal("130", stats.currentValue)
        assertDecimal("20", stats.profit)
        assertEquals(TradeType.BUY, stats.type)
    }

    @Test
    fun `transaction stats for a sale report proceeds and the realized result`() {
        val holdings = listOf(tx(TradeType.BUY, "2", "100"), tx(TradeType.SELL, "1", "160", fee = "10"))
        val coins = listOf(Coin(from = "BTC", to = USD, priceRaw = 200f))

        val stats = PortfolioCalculator.transactionStats(holdings.last(), holdings, coins)

        assertDecimal("150", stats.totalSpent)
        assertDecimal("50", stats.profit)
        assertDecimal("50", stats.profitPercent)
        assertEquals(TradeType.SELL, stats.type)
    }
}
