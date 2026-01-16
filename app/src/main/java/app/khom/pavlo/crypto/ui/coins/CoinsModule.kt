package app.khom.pavlo.crypto.ui.coins

import androidx.fragment.app.Fragment
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.network.NetworkRequests
import app.khom.pavlo.crypto.utils.Logger
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.Toaster
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.scopes.FragmentScoped


@InstallIn(FragmentComponent::class)
@Module
class CoinsModule {

    @Provides @FragmentScoped
    fun provideView(fragment: Fragment): ICoins.View = fragment as ICoins.View

    @Provides @FragmentScoped
    fun providePresenter(view: ICoins.View,
                         networkRequests: NetworkRequests,
                         coinsController: CoinsController,
                         db: CMDatabase,
                         resProvider: ResourceProvider,
                         pageController: PageController,
                         multiSelector: MultiSelector,
                         holdingsHandler: HoldingsHandler,
                         logger: Logger,
                         toaster: Toaster,
                         preferences: Preferences): ICoins.Presenter =
            CoinsPresenter(view, networkRequests, coinsController, db, resProvider,
                    pageController, multiSelector, holdingsHandler, logger, toaster, preferences)
}
