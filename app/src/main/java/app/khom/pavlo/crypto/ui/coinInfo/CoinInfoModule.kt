package app.khom.pavlo.crypto.ui.coinInfo

import android.app.Activity
import app.khom.pavlo.crypto.model.CoinsController
import app.khom.pavlo.crypto.model.GraphMaker
import app.khom.pavlo.crypto.model.HoldingsHandler
import app.khom.pavlo.crypto.model.network.NetworkRequests
import app.khom.pavlo.crypto.utils.Logger
import app.khom.pavlo.crypto.utils.ResourceProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped


@InstallIn(ActivityComponent::class)
@Module
class CoinInfoModule {

    @Provides @ActivityScoped
    fun provideView(activity: Activity): ICoinInfo.View = activity as ICoinInfo.View

    @Provides @ActivityScoped
    fun providePresenter(view: ICoinInfo.View,
                         coinsController: CoinsController,
                         networkRequests: NetworkRequests,
                         graphMaker: GraphMaker,
                         holdingsHandler: HoldingsHandler,
                         resourceProvider: ResourceProvider,
                         logger: Logger): ICoinInfo.Presenter =
            CoinInfoPresenter(view, coinsController, networkRequests, graphMaker, holdingsHandler, resourceProvider, logger)

}
