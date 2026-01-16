package app.khom.pavlo.crypto.ui.topCoins

import androidx.fragment.app.Fragment
import app.khom.pavlo.crypto.model.CoinsController
import app.khom.pavlo.crypto.model.PageController
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
class TopCoinsModule {

    @Provides @FragmentScoped
    fun provideView(fragment: Fragment): ITopCoins.View = fragment as ITopCoins.View

    @Provides @FragmentScoped
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
