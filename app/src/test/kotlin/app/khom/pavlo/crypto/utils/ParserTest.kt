package app.khom.pavlo.crypto.utils

import app.khom.pavlo.crypto.model.DATA
import app.khom.pavlo.crypto.model.AllCoinsResponse
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserTest {

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
}
