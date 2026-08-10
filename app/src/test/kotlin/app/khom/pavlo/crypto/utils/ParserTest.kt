package app.khom.pavlo.crypto.utils

import app.khom.pavlo.crypto.model.DATA
import app.khom.pavlo.crypto.model.AllCoinsResponse
import app.khom.pavlo.crypto.model.CoinPaprikaQuote
import app.khom.pavlo.crypto.model.CoinPaprikaQuotes
import app.khom.pavlo.crypto.model.CoinPaprikaTicker
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import kotlin.test.assertFailsWith

class ParserTest {

    @Test
    fun `number formatting is locale independent and rejects invalid values`() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("12,345.6789", addCommasToStringNumber("12345.6789"))
            assertEquals("12.3457", getStringWithTwoDecimalsFromDouble(12.34567f))
            assertEquals("", addCommasToStringNumber("NaN"))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `summary coin list maps symbol and full name`() {
        val response = Gson().fromJson(
            """
            {
              "Response": "Success",
              "Message": "",
              "BaseImageUrl": "https://images.example.com",
              "BaseLinkUrl": "",
              "Data": {
                "BTC": {
                  "Id": "1182",
                  "Symbol": "BTC",
                  "FullName": "Bitcoin",
                  "ImageUrl": "/btc.png"
                }
              },
              "Type": 100
            }
            """.trimIndent(),
            AllCoinsResponse::class.java
        )

        val coin = getAllCoinsFromJson(response).single()

        assertEquals("BTC", coin.name)
        assertEquals("Bitcoin", coin.coinName)
        assertEquals("https://images.example.com/btc.png", coin.imageUrl)
    }

    @Test
    fun `summary coin list keeps missing image empty`() {
        val response = Gson().fromJson(
            """
            {
              "Response": "Success",
              "BaseImageUrl": "https://images.example.com/",
              "Data": {
                "BTC": {
                  "Symbol": "BTC",
                  "FullName": "Bitcoin",
                  "ImageUrl": ""
                }
              }
            }
            """.trimIndent(),
            AllCoinsResponse::class.java
        )

        assertEquals("", getAllCoinsFromJson(response).single().imageUrl)
    }

    @Test
    fun `summary coin list preserves absolute image url`() {
        val response = Gson().fromJson(
            """
            {
              "Response": "Success",
              "BaseImageUrl": "https://images.example.com",
              "Data": {
                "BTC": {
                  "Symbol": "BTC",
                  "FullName": "Bitcoin",
                  "ImageUrl": "https://cdn.example.com/btc.png"
                }
              }
            }
            """.trimIndent(),
            AllCoinsResponse::class.java
        )

        assertEquals("https://cdn.example.com/btc.png", getAllCoinsFromJson(response).single().imageUrl)
    }

    @Test
    fun `news parser accepts CoinDesk article fields`() {
        val json = JsonParser.parseString(
            """
            {
              "articles": [
                {
                  "TITLE": "Bitcoin update",
                  "BODY": "Market body",
                  "URL": "https://example.com/story",
                  "SOURCE_DATA_NAME": "Example",
                  "PUBLISHED_ON": 1700000000,
                  "IMAGE_URL": "https://example.com/image.png"
                }
              ]
            }
            """.trimIndent()
        ).asJsonObject

        val result = getNewsFromJson(json)

        assertEquals(1, result.size)
        assertEquals("Bitcoin update", result.single().title)
        assertEquals("Example", result.single().source)
        assertEquals(1700000000L, result.single().publishedOn)
    }

    @Test
    fun `news RSS parser maps article text image and date`() {
        val xml = """
            <rss xmlns:media="http://search.yahoo.com/mrss/" xmlns:dc="http://purl.org/dc/elements/1.1/">
              <channel>
                <item>
                  <title><![CDATA[Bitcoin &amp; markets]]></title>
                  <description><![CDATA[<p>Market <b>update</b></p>]]></description>
                  <link>https://example.com/story</link>
                  <dc:creator>CoinDesk Markets</dc:creator>
                  <pubDate>Sun, 09 Aug 2026 12:30:00 +0000</pubDate>
                  <media:content url="https://example.com/image.jpg" />
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val item = getNewsFromRss(xml).single()

        assertEquals("Bitcoin & markets", item.title)
        assertEquals("Market update", item.body)
        assertEquals("CoinDesk Markets", item.source)
        assertEquals("https://example.com/image.jpg", item.imageUrl)
        assertTrue(item.publishedOn > 0L)
    }

    @Test
    fun `top coins parser skips malformed entries and assigns contiguous ranks`() {
        val json = JsonParser.parseString(
            """
            {
              "$DATA": [
                {"CoinInfo":{"Name":"BTC","FullName":"Bitcoin","Id":"1"},"RAW":{"USD":{"PRICE":10}}},
                {"CoinInfo":{}},
                {"CoinInfo":{"Name":"ETH","FullName":"Ethereum","Id":"2"},"RAW":{"USD":{"PRICE":5}}}
              ]
            }
            """.trimIndent()
        ).asJsonObject

        val result = getTopCoinsFromJson(json)

        assertEquals(listOf("BTC", "ETH"), result.map { it.symbol })
        assertEquals(listOf(1, 2), result.map { it.rank })
        assertTrue(result.all { it.name.isNotBlank() })
    }

    @Test
    fun `top coins parser exposes CryptoCompare error responses`() {
        val json = JsonParser.parseString(
            """{"Response":"Error","Message":"Rate limit exceeded"}"""
        ).asJsonObject

        val error = assertFailsWith<IllegalStateException> { getTopCoinsFromJson(json) }

        assertEquals("Rate limit exceeded", error.message)
    }

    @Test
    fun `CoinPaprika fallback response deserializes every required field`() {
        val ticker = Gson().fromJson(
            """
            [
              {
                "id": "btc-bitcoin",
                "name": "Bitcoin",
                "symbol": "BTC",
                "rank": 1,
                "total_supply": 21000000,
                "last_updated": "2026-08-09T00:00:00Z",
                "quotes": {
                  "USD": {
                    "price": 100000,
                    "volume_24h": 50000000000,
                    "market_cap": 2000000000000,
                    "percent_change_1h": 0.5,
                    "percent_change_24h": 2.5,
                    "percent_change_7d": 4.5
                  }
                }
              }
            ]
            """.trimIndent(),
            Array<CoinPaprikaTicker>::class.java
        ).single()

        assertEquals("btc-bitcoin", ticker.id)
        assertEquals("BTC", ticker.symbol)
        assertEquals(1, ticker.rank)
        assertEquals(100_000.0, ticker.quotes?.usd?.price ?: 0.0, 0.0)
        assertEquals(2.5, ticker.quotes?.usd?.percent_change_24h ?: 0.0, 0.0)
    }

    @Test
    fun `CoinPaprika tickers map and sort by rank`() {
        val tickers = listOf(
            CoinPaprikaTicker(
                id = "eth-ethereum",
                name = "Ethereum",
                symbol = "eth",
                rank = 2,
                total_supply = 120_000_000.0,
                quotes = CoinPaprikaQuotes(CoinPaprikaQuote(price = 3_000.0))
            ),
            CoinPaprikaTicker(
                id = "btc-bitcoin",
                name = "Bitcoin",
                symbol = "btc",
                rank = 1,
                total_supply = 21_000_000.0,
                quotes = CoinPaprikaQuotes(
                    CoinPaprikaQuote(
                        price = 100_000.0,
                        volume_24h = 50_000_000_000.0,
                        market_cap = 2_000_000_000_000.0,
                        percent_change_24h = 2.5
                    )
                )
            )
        )

        val result = getTopCoinsFromCoinPaprika(tickers)

        assertEquals(listOf("BTC", "ETH"), result.map { it.symbol })
        assertEquals(listOf(1, 2), result.map { it.rank })
        assertEquals("100000.0", result.first().price_usd)
        assertEquals("2.5", result.first().percent_change_24h)
        assertEquals(
            "https://static.coinpaprika.com/coin/btc-bitcoin/logo.png",
            result.first().imgUrl
        )
    }

    @Test
    fun `CoinPaprika ticker converts to a complete favorite coin`() {
        val ticker = CoinPaprikaTicker(
            id = "btc-bitcoin",
            name = "Bitcoin",
            symbol = "btc",
            rank = 1,
            total_supply = 21_000_000.0,
            last_updated = "2026-08-09T00:00:00Z",
            quotes = CoinPaprikaQuotes(
                CoinPaprikaQuote(
                    price = 100_000.0,
                    volume_24h = 50_000_000_000.0,
                    market_cap = 2_000_000_000_000.0,
                    percent_change_24h = 2.5
                )
            )
        )

        val favorite = getCoinsFromCoinPaprika(listOf(ticker), listOf("BTC")).single()

        assertEquals("BTC", favorite.from)
        assertEquals("USD", favorite.to)
        assertEquals("Bitcoin", favorite.fullName)
        assertEquals(100_000f, favorite.priceRaw)
        assertEquals(2.5f, favorite.changePct24hRaw)
        assertEquals("https://static.coinpaprika.com/coin/btc-bitcoin/logo.png", favorite.imgUrl)
    }

    @Test
    fun `CoinPaprika tickers provide searchable coin catalog`() {
        val ticker = CoinPaprikaTicker(
            id = "btc-bitcoin",
            name = "Bitcoin",
            symbol = "btc",
            rank = 1
        )

        val info = getAllCoinsFromCoinPaprika(listOf(ticker)).single()

        assertEquals("btc-bitcoin", info.coinId)
        assertEquals("BTC", info.name)
        assertEquals("Bitcoin", info.coinName)
        assertEquals("https://static.coinpaprika.com/coin/btc-bitcoin/logo.png", info.imageUrl)
    }

    @Test
    fun `price parser exposes CryptoCompare rate limit response`() {
        val json = JsonParser.parseString(
            """{"Response":"Error","Message":"You are over your rate limit"}"""
        ).asJsonObject

        val error = assertFailsWith<IllegalStateException> {
            getCoinsFromJson(json, emptyMap())
        }

        assertEquals("You are over your rate limit", error.message)
    }

    @Test
    fun `historical parser accepts CryptoCompare candles`() {
        val json = JsonParser.parseString(
            """
            {
              "Data": [
                {
                  "time": 1700000000,
                  "open": 100.5,
                  "high": 110.0,
                  "low": 99.0,
                  "close": 108.25,
                  "volumefrom": 12.0,
                  "volumeto": 1300.0
                }
              ]
            }
            """.trimIndent()
        ).asJsonObject

        val candle = getHistoListFromJson(json).single()

        assertEquals(1700000000L, candle.time)
        assertEquals(100.5f, candle.open)
        assertEquals(108.25f, candle.close)
    }

    @Test
    fun `historical parser accepts CoinDesk uppercase candles`() {
        val json = JsonParser.parseString(
            """
            {
              "Data": [
                {
                  "TIMESTAMP": 1700003600,
                  "OPEN": 200.0,
                  "HIGH": 220.0,
                  "LOW": 190.0,
                  "CLOSE": 215.0,
                  "VOLUME": 42.0
                }
              ],
              "Err": {}
            }
            """.trimIndent()
        ).asJsonObject

        val candle = getHistoListFromJson(json).single()

        assertEquals(1700003600L, candle.time)
        assertEquals(220f, candle.high)
        assertEquals(190f, candle.low)
        assertEquals(42f, candle.volumeFrom)
    }

    @Test
    fun `Coinbase historical parser maps and sorts candles`() {
        val candles = listOf(
            listOf(1_700_003_600.0, 105.0, 115.0, 108.0, 112.0, 4.0),
            listOf(1_700_000_000.0, 99.0, 110.0, 100.5, 108.25, 12.0)
        )

        val result = getHistoListFromCoinbase(candles)

        assertEquals(listOf(1_700_000_000L, 1_700_003_600L), result.map { it.time })
        assertEquals(100.5f, result.first().open)
        assertEquals(108.25f, result.first().close)
        assertEquals(1_299f, result.first().volumeTo)
    }

    @Test
    fun `Coinbase historical parser ignores malformed candles`() {
        val result = getHistoListFromCoinbase(
            listOf(
                listOf(1_700_000_000.0, 99.0),
                listOf(1_700_000_000.0, 99.0, 110.0, Double.NaN, 108.25, 12.0)
            )
        )

        assertTrue(result.isEmpty())
    }
}