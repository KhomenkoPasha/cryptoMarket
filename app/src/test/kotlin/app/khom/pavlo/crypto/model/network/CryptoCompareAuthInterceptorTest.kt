package app.khom.pavlo.crypto.model.network

import okhttp3.Request
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CryptoCompareAuthInterceptorTest {

    @Test
    fun `adds authorization to CryptoCompare requests`() {
        val request = request("https://min-api.cryptocompare.com/data/top/totalvolfull")

        val authenticated = request.withCryptoCompareAuthorization("test-key")

        assertEquals("Apikey test-key", authenticated.header("authorization"))
    }

    @Test
    fun `does not send CryptoCompare key to another host`() {
        val request = request("https://data-api.coindesk.com/news/v1/article/list")

        val untouched = request.withCryptoCompareAuthorization("test-key")

        assertNull(untouched.header("authorization"))
    }

    @Test
    fun `does not add an empty key`() {
        val request = request("https://min-api.cryptocompare.com/data/top/totalvolfull")

        val untouched = request.withCryptoCompareAuthorization("")

        assertNull(untouched.header("authorization"))
    }

    private fun request(url: String): Request = Request.Builder().url(url).build()
}
