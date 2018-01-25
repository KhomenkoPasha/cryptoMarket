package app.khom.pavlo.crypto.ui.addCoin

import app.khom.pavlo.crypto.di.PerActivity
import app.khom.pavlo.crypto.model.CoinsController
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.network.NetworkRequests
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.Toaster
import dagger.Module
import dagger.Provides

/**
 * Created by rmnivnv on 27/07/2017.
 */
@Module
class AddCoinModule {

    @Provides @PerActivity
    fun provideView(addCoinActivity: AddCoinActivity): IAddCoin.View = addCoinActivity

    @Provides @PerActivity
    fun providePresenter(view: IAddCoin.View,
                         coinsController: CoinsController,
                         networkRequests: NetworkRequests,
                         resProvider: ResourceProvider,
                         db: CMDatabase,
                         toaster: Toaster): IAddCoin.Presenter =
            AddCoinPresenter(view, coinsController, networkRequests, resProvider, db, toaster)
}