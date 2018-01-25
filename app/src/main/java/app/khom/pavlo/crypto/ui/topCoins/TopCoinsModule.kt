package app.khom.pavlo.crypto.ui.topCoins

import app.khom.pavlo.crypto.di.PerFragment
import app.khom.pavlo.crypto.model.CoinsController
import app.khom.pavlo.crypto.model.PageController
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.network.NetworkRequests
import app.khom.pavlo.crypto.utils.Logger
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.Toaster
import dagger.Module
import dagger.Provides

/**
 * Created by rmnivnv on 02/09/2017.
 */
@Module
class TopCoinsModule {

    @Provides @PerFragment
    fun provideView(topCoinsFragment: TopCoinsFragment): ITopCoins.View = topCoinsFragment

    @Provides @PerFragment
    fun providePresenter(view: ITopCoins.View,
                         db: CMDatabase,
                         networkRequests: NetworkRequests,
                         coinsController: CoinsController,
                         resProvider: ResourceProvider,
                         pageController: PageController,
                         toaster: Toaster,
                         logger: Logger): ITopCoins.Presenter =
            TopCoinsPresenter(view, db, networkRequests, coinsController, resProvider, pageController, toaster, logger)
}