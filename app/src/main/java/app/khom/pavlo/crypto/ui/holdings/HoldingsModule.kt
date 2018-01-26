package app.khom.pavlo.crypto.ui.holdings

import app.khom.pavlo.crypto.di.PerActivity
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.Toaster
import dagger.Module
import dagger.Provides


@Module
class HoldingsModule {

    @Provides @PerActivity
    fun provideView(holdingsActivity: HoldingsActivity): IHoldings.View = holdingsActivity

    @Provides @PerActivity
    fun providePresenter(view: IHoldings.View,
                         db: CMDatabase,
                         resourceProvider: ResourceProvider,
                         toaster: Toaster): IHoldings.Presenter =
            HoldingsPresenter(view, db, resourceProvider, toaster)

}