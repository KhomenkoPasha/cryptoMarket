package app.khom.pavlo.crypto.ui.portfolio

import app.khom.pavlo.crypto.model.FSYMS
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.TSYMS
import app.khom.pavlo.crypto.model.USD
import org.junit.Assert.assertEquals
import org.junit.Test

class PortfolioPriceQueryTest {

    @Test
    fun `query contains each portfolio coin only once`() {
        val holdings = listOf(
            holding("btc-bitcoin", "BTC"),
            holding("btc-bitcoin", "BTC"),
            holding("btc-bitcoin", "BTC"),
            holding("eth-ethereum", "ETH"),
            holding("eth-ethereum", "ETH"),
            holding("sol-solana", "SOL")
        )

        val query = portfolioPriceQuery(holdings)

        assertEquals(listOf("BTC", "ETH", "SOL"), query[FSYMS])
        assertEquals(listOf(USD), query[TSYMS])
    }

    private fun holding(coinId: String, symbol: String) = HoldingData(
        from = symbol,
        to = USD,
        date = 1,
        coinId = coinId
    )
}