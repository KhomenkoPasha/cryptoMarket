package app.khom.pavlo.crypto.rest

import app.khom.pavlo.crypto.models.ResponseGlobalData
import io.reactivex.Observable
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface RetrofitGetInterfaceGlobal {
    @GET("v1/global//")
    fun getCryptocurrencyGlobal(): Observable<ResponseGlobalData>


    companion object Factory {
        fun create(): RetrofitGetInterfaceGlobal {
            val retrofit = Retrofit.Builder()
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(" https://api.coinmarketcap.com/")
                    .build()

            return retrofit.create(RetrofitGetInterfaceGlobal::class.java)
        }
    }
}