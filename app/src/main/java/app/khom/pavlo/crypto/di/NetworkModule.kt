package app.khom.pavlo.crypto.di

import app.khom.pavlo.crypto.model.BASE_CRYPTOCOMPARE_URL
import app.khom.pavlo.crypto.model.network.CoinMarketCapApi
import app.khom.pavlo.crypto.model.network.CryptoCompareAPI
import app.khom.pavlo.crypto.model.network.NetworkRequests
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
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
