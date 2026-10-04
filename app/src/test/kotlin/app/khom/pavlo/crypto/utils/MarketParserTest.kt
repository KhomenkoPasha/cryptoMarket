package app.khom.pavlo.crypto.utils

import app.khom.pavlo.crypto.R
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketParserTest {

    private fun json(text: String) = JsonParser.parseString(text).asJsonObject

    @Test
    fun `cryptocompare rates keep supported currencies only`() {
        val rates = getFxRatesFromCryptoCompare(json("""{"EUR":0.92,"UAH":41.5,"RUB":92,"BTC":0.0000117,"JPY":150}"""))
        assertEquals(setOf("EUR", "UAH", "RUB", "BTC"), rates.keys)
        assertEquals(0.92, rates.getValue("EUR"), 0.0)
    }

    @Test(expected = IllegalStateException::class)
    fun `cryptocompare error responses are surfaced`() {
        getFxRatesFromCryptoCompare(json("""{"Response":"Error","Message":"rate limit"}"""))
    }

    @Test
    fun `coinbase rates are parsed from strings`() {
        val rates = getFxRatesFromCoinbase(json("""{"data":{"currency":"USD","rates":{"EUR":"0.92","UAH":"41.5","BTC":"0.0000117"}}}"""))
        assertEquals(0.92, rates.getValue("EUR"), 0.0)
        assertEquals(41.5, rates.getValue("UAH"), 0.0)
        assertTrue(!rates.containsKey("RUB"))
    }

    @Test
    fun `coinbase payloads without rates yield nothing`() {
        assertTrue(getFxRatesFromCoinbase(json("""{"data":{}}""")).isEmpty())
        assertTrue(getFxRatesFromCoinbase(json("""{}""")).isEmpty())
    }

    @Test
    fun `fear and greed reads the latest and previous value`() {
        val index = getFearGreedFromJson(
            json("""{"data":[{"value":"72","value_classification":"Greed","timestamp":"1700000000"},{"value":"68"}]}""")
        )
        assertNotNull(index)
        assertEquals(72, index!!.value)
        assertEquals(68, index.previousValue)
        assertEquals(1_700_000_000L, index.timestamp)
    }

    @Test
    fun `fear and greed rejects out of range or missing values`() {
        assertNull(getFearGreedFromJson(json("""{"data":[{"value":"250"}]}""")))
        assertNull(getFearGreedFromJson(json("""{"data":[]}""")))
        assertNull(getFearGreedFromJson(json("""{}""")))
    }

    @Test
    fun `global market stats require at least one usable figure`() {
        val stats = getGlobalMarketFromJson(
            json("""{"bitcoin_dominance_percentage":56.33,"market_cap_usd":3.04e12,"volume_24h_usd":1.2e11,"market_cap_change_24h":0.61}""")
        )
        assertNotNull(stats)
        assertEquals(56.33, stats!!.btcDominance!!, 0.0)
        assertEquals(0.61, stats.marketCapChange24h!!, 0.0)
        assertNull(getGlobalMarketFromJson(json("""{"unrelated":1}""")))
    }

    @Test
    fun `fear and greed zones map to the five classic bands`() {
        assertEquals(R.string.fng_extreme_fear, fearGreedLabel(0))
        assertEquals(R.string.fng_extreme_fear, fearGreedLabel(24))
        assertEquals(R.string.fng_fear, fearGreedLabel(25))
        assertEquals(R.string.fng_fear, fearGreedLabel(44))
        assertEquals(R.string.fng_neutral, fearGreedLabel(45))
        assertEquals(R.string.fng_neutral, fearGreedLabel(55))
        assertEquals(R.string.fng_greed, fearGreedLabel(56))
        assertEquals(R.string.fng_greed, fearGreedLabel(65))
        assertEquals(R.string.fng_greed, fearGreedLabel(75))
        assertEquals(R.string.fng_extreme_greed, fearGreedLabel(76))
        assertEquals(R.string.fng_extreme_greed, fearGreedLabel(100))
    }
}
