package app.khom.pavlo.crypto.model.network

import com.google.gson.JsonObject
import app.khom.pavlo.crypto.model.AllCoinsResponse
import app.khom.pavlo.crypto.model.CoinPaprikaTicker
import io.reactivex.rxjava3.core.Single
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url


interface CryptoCompareAPI {

    @GET("all/coinlist")
    fun getCoinsList(@Query("summary") summary: Boolean): Single<AllCoinsResponse>

    @GET("pricemultifull")
    fun getPrice(@Query("fsyms") from: String, @Query("tsyms") to: String): Single<JsonObject>

    @GET("v2/{period}")
    fun getHistoPeriod(@Path("period") period: String,
                       @Query("fsym") from: String?,
                       @Query("tsym") to: String?,
                       @Query("limit") limit: Int,
                       @Query("aggregate") aggregate: Int): Single<JsonObject>

    @GET
    fun getCoinDeskHistoPeriod(
            @Url url: String,
            @Query("market") market: String,
            @Query("instrument") instrument: String,
            @Query("limit") limit: Int,
            @Query("aggregate") aggregate: Int,
            @Query("groups") groups: String,
            @Query("api_key") apiKey: String?
    ): Single<JsonObject>

    @GET
    fun getCoinbaseCandles(
            @Url url: String,
            @Query("granularity") granularity: Int
    ): Single<List<List<Double>>>

    @GET("top/pairs")
    fun getPairs(@Query("fsym") from: String): Single<JsonObject>

    @GET("top/totalvolfull")
    fun getTopCoins(@Query("limit") limit: Int, @Query("tsym") toSymbol: String): Single<JsonObject>

    @GET
    fun getCoinPaprikaTickers(
            @Url url: String,
            @Query("quotes") quote: String
    ): Single<List<CoinPaprikaTicker>>

    @GET
    fun getNews(
            @Url url: String,
            @Query("lang") lang: String,
            @Query("categories") categories: String?,
            @Query("limit") limit: Int,
            @Query("api_key") apiKey: String?
    ): Single<JsonObject>

    @GET
    fun getNewsRss(@Url url: String): Single<ResponseBody>
}