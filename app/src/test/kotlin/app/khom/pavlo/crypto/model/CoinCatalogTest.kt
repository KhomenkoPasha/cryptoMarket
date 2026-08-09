package app.khom.pavlo.crypto.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CoinCatalogTest {

    @Test
    fun `prefers ranked canonical Solana over duplicate SOL assets`() {
        val catalog = listOf(
            infoCoin("sol-binance-peg-sol", "SOL", "Binance-Peg SOL", "259"),
            infoCoin("sol-solana", "SOL", "Solana", "7"),
            infoCoin("usol-wrapped-solana-universal", "USOL", "Wrapped Solana", "1200")
        )

        val preferred = preferredCoinInfoBySymbol(catalog)

        assertEquals("sol-solana", preferred.getValue("SOL").coinId)
        assertEquals("Solana", preferred.getValue("SOL").coinName)
        assertEquals("usol-wrapped-solana-universal", preferred.getValue("USOL").coinId)
    }

    private fun infoCoin(id: String, symbol: String, name: String, rank: String) = InfoCoin(
        coinId = id,
        name = symbol,
        coinName = name,
        fullName = name,
        sortOrder = rank
    )
}