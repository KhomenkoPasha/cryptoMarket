package app.khom.pavlo.crypto.ui.addCoin

import android.app.Activity
import app.khom.pavlo.crypto.model.CoinsController
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.network.NetworkRequests
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.Toaster
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped


@Module
@InstallIn(ActivityComponent::class)
class AddCoinModule {

    @Provides @ActivityScoped
    fun provideView(activity: Activity): IAddCoin.View = activity as IAddCoin.View

    @Provides @ActivityScoped
    fun providePresenter(view: IAddCoin.View,
                         coinsController: CoinsController,
                         networkRequests: NetworkRequests,
                         resProvider: ResourceProvider,
                         db: CMDatabase,
                         toaster: Toaster): IAddCoin.Presenter =
            AddCoinPresenter(view, coinsController, networkRequests, resProvider, db, toaster)
}
