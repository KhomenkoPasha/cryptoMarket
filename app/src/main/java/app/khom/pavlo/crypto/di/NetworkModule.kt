package app.khom.pavlo.crypto.di

import app.khom.pavlo.crypto.rest.CoinMarketCapApi
import app.khom.pavlo.crypto.rest.CryptoCompareAPI
import app.khom.pavlo.crypto.rest.NetworkRequests
import app.khom.pavlo.crypto.utills.BASE_CRYPTOCOMPARE_URL
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
class NetworkModule {

    @Provides @Singleton
    fun provideRetrofit(): Retrofit =
            Retrofit.Builder()
                    .baseUrl(BASE_CRYPTOCOMPARE_URL)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

    @Provides @Singleton
    fun provideCrComApi(retrofit: Retrofit): CryptoCompareAPI = retrofit.create(CryptoCompareAPI::class.java)

    @Provides @Singleton
    fun provideCoinMarketCapApi(retrofit: Retrofit): CoinMarketCapApi = retrofit.create(CoinMarketCapApi::class.java)

    @Provides @Singleton
    fun provideNetworkRequests(cryptoCompareAPI: CryptoCompareAPI, coinMarketCapApi: CoinMarketCapApi) =
            NetworkRequests(cryptoCompareAPI, coinMarketCapApi)
}