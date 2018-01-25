package app.khom.pavlo.crypto.ui.coinAllocation

import app.khom.pavlo.crypto.di.PerActivity
import app.khom.pavlo.crypto.model.PieMaker
import app.khom.pavlo.crypto.model.db.CMDatabase
import dagger.Module
import dagger.Provides


@Module
class CoinAllocationModule {

    @Provides
    @PerActivity
    fun provideView(coinAllocationActivity: CoinAllocationActivity): ICoinAllocation.View = coinAllocationActivity

    @Provides
    @PerActivity
    fun providePresenter(view: ICoinAllocation.View,
                         pieMaker: PieMaker,
                         db: CMDatabase): ICoinAllocation.Presenter = CoinAllocationPresenter( view, pieMaker, db)
}