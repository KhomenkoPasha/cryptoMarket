package app.khom.pavlo.crypto.di

import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkModuleTest {

    @Test
    fun okHttpClientUsesThirtySecondTimeouts() {
        val client = NetworkModule().provideOkHttpClient()
        val expectedTimeoutMillis = 30_000

        assertEquals(expectedTimeoutMillis, client.connectTimeoutMillis)
        assertEquals(expectedTimeoutMillis, client.readTimeoutMillis)
        assertEquals(expectedTimeoutMillis, client.writeTimeoutMillis)
        assertEquals(expectedTimeoutMillis, client.callTimeoutMillis)
    }
}