package app.khom.pavlo.crypto.ui.news

import app.khom.pavlo.crypto.model.Preferences
import org.junit.Assert.assertEquals
import org.junit.Test

class NewsSearchTest {

    private val news = listOf(
        NewsItem(
            title = "Bitcoin adoption grows",
            body = "Institutional demand pushes the market higher",
            url = "https://example.com/bitcoin",
            source = "CoinDesk",
            publishedOn = 1L,
            imageUrl = ""
        ),
        NewsItem(
            title = "Ethereum network update",
            body = "Developers prepare the next upgrade",
            url = "https://example.com/ethereum",
            source = "Decrypt",
            publishedOn = 2L,
            imageUrl = ""
        )
    )

    @Test
    fun `normalization removes hashtag and extra whitespace`() {
        assertEquals("Bitcoin market", normalizeNewsQuery("  #Bitcoin   market  "))
    }

    @Test
    fun `filter is case insensitive and searches title body source and url`() {
        assertEquals(news.take(1), filterNewsItems(news, "BITCOIN"))
        assertEquals(news.take(1), filterNewsItems(news, "institutional"))
        assertEquals(news.take(1), filterNewsItems(news, "coindesk"))
        assertEquals(news.drop(1), filterNewsItems(news, "example.com/ethereum"))
    }

    @Test
    fun `all query tokens must match the same article`() {
        assertEquals(news.take(1), filterNewsItems(news, "#bitcoin demand"))
        assertEquals(emptyList<NewsItem>(), filterNewsItems(news, "bitcoin developers"))
    }

    @Test
    fun `empty query returns all news`() {
        assertEquals(news, filterNewsItems(news, " # "))
    }

    @Test
    fun `default news query does not filter the feed`() {
        assertEquals("", Preferences.SEARCH_HASH_TAG_DEFAULT)
        assertEquals(news, filterNewsItems(news, Preferences.SEARCH_HASH_TAG_DEFAULT))
    }
}