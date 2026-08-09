package app.khom.pavlo.crypto.ui.holdings

import app.khom.pavlo.crypto.model.Coin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PortfolioCoinOptionsTest {

    private val solana = Coin(from = "SOL", to = "USD", fullName = "Solana")

    @Test
    fun `finds coin by full name symbol and displayed label`() {
        val coins = listOf(solana)

        assertEquals(solana, findPortfolioCoinForInput(coins, "Solana"))
        assertEquals(solana, findPortfolioCoinForInput(coins, "sol"))
        assertEquals(solana, findPortfolioCoinForInput(coins, "Solana (SOL) / USD"))
    }

    @Test
    fun `does not accept a partial or unknown name`() {
        assertNull(findPortfolioCoinForInput(listOf(solana), "Sola"))
        assertNull(findPortfolioCoinForInput(listOf(solana), "unknown"))
    }

    @Test
    fun `typing does not select an unrelated one letter symbol too early`() {
        val sonic = Coin(from = "S", to = "USD", fullName = "Sonic")

        assertNull(findPortfolioCoinForInput(listOf(sonic, solana), "S", allowSymbol = false))
        assertEquals(solana, findPortfolioCoinForInput(listOf(sonic, solana), "Solana", allowSymbol = false))
    }
}