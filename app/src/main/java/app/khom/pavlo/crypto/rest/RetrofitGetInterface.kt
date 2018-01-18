package  app.khom.pavlo.crypto.rest

import app.khom.pavlo.crypto.models.ResponseCoinItem
import io.reactivex.Observable
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET


interface RetrofitGetInterface {
    @GET("v1/ticker/")
    fun getCryptocurrency(): Observable<List<ResponseCoinItem>>


    companion object Factory {
        fun create(): RetrofitGetInterface {
            val retrofit = Retrofit.Builder()
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(" https://api.coinmarketcap.com/")
                    .build()

            return retrofit.create(RetrofitGetInterface::class.java)
        }
    }
}