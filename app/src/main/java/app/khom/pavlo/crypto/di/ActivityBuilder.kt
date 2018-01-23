package app.khom.pavlo.crypto.di

import app.khom.pavlo.crypto.activities.StartCryptoActivity
import app.khom.pavlo.crypto.coinInfo.CoinInfoActivity
import app.khom.pavlo.crypto.coinInfo.CoinInfoModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityBuilder {

    @PerActivity
    @ContributesAndroidInjector(modules = arrayOf(CoinInfoModule::class))
    abstract fun bindCoinInfoActivity(): CoinInfoActivity

}