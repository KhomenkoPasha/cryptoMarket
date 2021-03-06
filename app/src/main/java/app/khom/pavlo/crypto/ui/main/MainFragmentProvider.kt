package app.khom.pavlo.crypto.ui.main

import app.khom.pavlo.crypto.di.PerFragment
import app.khom.pavlo.crypto.ui.coins.CoinsFragment
import app.khom.pavlo.crypto.ui.coins.CoinsModule
import app.khom.pavlo.crypto.ui.news.NewsFragment
import app.khom.pavlo.crypto.ui.news.NewsModule
import app.khom.pavlo.crypto.ui.topCoins.TopCoinsFragment
import app.khom.pavlo.crypto.ui.topCoins.TopCoinsModule
import dagger.Module
import dagger.android.ContributesAndroidInjector


@Module
abstract class MainFragmentProvider {

    @PerFragment
    @ContributesAndroidInjector(modules = arrayOf(CoinsModule::class))
    abstract fun provideCoinFragmentFactory(): CoinsFragment

    @PerFragment
    @ContributesAndroidInjector(modules = arrayOf(TopCoinsModule::class))
    abstract fun provideTopCoinsFragmentFactory(): TopCoinsFragment

    @PerFragment
    @ContributesAndroidInjector(modules = arrayOf(NewsModule::class))
    abstract fun provideNewsFragmentFactory(): NewsFragment

}