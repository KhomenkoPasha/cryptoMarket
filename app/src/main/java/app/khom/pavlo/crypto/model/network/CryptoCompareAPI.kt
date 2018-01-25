package app.khom.pavlo.crypto.model.network

import com.google.gson.JsonObject
import app.khom.pavlo.crypto.model.AllCoinsResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url


interface CryptoCompareAPI {

    @GET
    fun getCoinsList(@Url url: String): Single<AllCoinsResponse>

    @GET("pricemultifull")
    fun getPrice(@Query("fsyms") from: String, @Query("tsyms") to: String): Single<JsonObject>

    @GET("{period}")
    fun getHistoPeriod(@Path("period") period: String,
                       @Query("fsym") from: String?,
                       @Query("tsym") to: String?,
                       @Query("limit") limit: Int,
                       @Query("aggregate") aggregate: Int): Single<JsonObject>

    @GET("top/pairs")
    fun getPairs(@Query("fsym") from: String): Single<JsonObject>
}