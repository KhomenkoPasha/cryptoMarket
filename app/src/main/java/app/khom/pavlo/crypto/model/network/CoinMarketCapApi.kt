package app.khom.pavlo.crypto.model.network

import app.khom.pavlo.crypto.model.TopCoinData
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url


interface CoinMarketCapApi {

    @GET
    fun getTopCoins(@Url url: String,  @Query("limit") limit: Int): Single<List<TopCoinData>>
}