package app.khom.pavlo.crypto.di

import app.khom.pavlo.crypto.BuildConfig
import app.khom.pavlo.crypto.model.BASE_CRYPTOCOMPARE_URL
import app.khom.pavlo.crypto.model.network.CryptoCompareAuthInterceptor
import app.khom.pavlo.crypto.model.network.CryptoCompareAPI
import app.khom.pavlo.crypto.model.network.NetworkRequests
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class NetworkModule {

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                    .addInterceptor(CryptoCompareAuthInterceptor(BuildConfig.CRYPTOCOMPARE_API_KEY))
                    .build()

    @Provides @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
            Retrofit.Builder()
                    .baseUrl(BASE_CRYPTOCOMPARE_URL)
                    .client(okHttpClient)
                    .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

    @Provides @Singleton
    fun provideCrComApi(retrofit: Retrofit): CryptoCompareAPI = retrofit.create(CryptoCompareAPI::class.java)

    @Provides @Singleton
    fun provideNetworkRequests(cryptoCompareAPI: CryptoCompareAPI) = NetworkRequests(cryptoCompareAPI)
}
