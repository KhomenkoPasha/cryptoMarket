package app.khom.pavlo.crypto.model.network

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal const val CRYPTOCOMPARE_API_HOST = "min-api.cryptocompare.com"
private const val AUTHORIZATION_HEADER = "authorization"

class CryptoCompareAuthInterceptor(private val apiKey: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response =
            chain.proceed(chain.request().withCryptoCompareAuthorization(apiKey))
}

internal fun Request.withCryptoCompareAuthorization(apiKey: String): Request {
    if (apiKey.isBlank() || !url.host.equals(CRYPTOCOMPARE_API_HOST, ignoreCase = true)) {
        return this
    }
    return newBuilder()
            .header(AUTHORIZATION_HEADER, "Apikey $apiKey")
            .build()
}
