package app.khom.pavlo.crypto.ui.coins

import app.khom.pavlo.crypto.di.PerFragment
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.network.NetworkRequests
import app.khom.pavlo.crypto.utils.Logger
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.Toaster
import dagger.Module
import dagger.Provides


@Module
class CoinsModule {

    @Provides @PerFragment
    fun provideView(coinsFragment: CoinsFragment): ICoins.View = coinsFragment

    @Provides @PerFragment
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