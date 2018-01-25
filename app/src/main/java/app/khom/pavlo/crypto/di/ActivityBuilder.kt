package app.khom.pavlo.crypto.di

import app.khom.pavlo.crypto.ui.addCoin.AddCoinActivity
import app.khom.pavlo.crypto.ui.addCoin.AddCoinModule
import app.khom.pavlo.crypto.ui.coinAllocation.CoinAllocationActivity
import app.khom.pavlo.crypto.ui.coinAllocation.CoinAllocationModule
import app.khom.pavlo.crypto.ui.coinInfo.CoinInfoActivity
import app.khom.pavlo.crypto.ui.coinInfo.CoinInfoModule
import app.khom.pavlo.crypto.ui.holdings.HoldingsActivity
import app.khom.pavlo.crypto.ui.holdings.HoldingsModule
import app.khom.pavlo.crypto.ui.main.MainActivity
import app.khom.pavlo.crypto.ui.main.MainFragmentProvider
import app.khom.pavlo.crypto.ui.main.MainModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityBuilder {

    @PerActivity
    @ContributesAndroidInjector(modules = arrayOf(MainModule::class, MainFragmentProvider::class))
    abstract fun bindMainActivity(): MainActivity

    @PerActivity
    @ContributesAndroidInjector(modules = arrayOf(AddCoinModule::class))
    abstract fun bindAddCoinActivity(): AddCoinActivity

    @PerActivity
    @ContributesAndroidInjector(modules = arrayOf(CoinInfoModule::class))
    abstract fun bindCoinInfoActivity(): CoinInfoActivity

    @PerActivity
    @ContributesAndroidInjector(modules = arrayOf(CoinAllocationModule::class))
    abstract fun bindCoinAllocationActivity(): CoinAllocationActivity

    @PerActivity
    @ContributesAndroidInjector(modules = arrayOf(HoldingsModule::class))
    abstract fun bindHoldingsActivity(): HoldingsActivity

}