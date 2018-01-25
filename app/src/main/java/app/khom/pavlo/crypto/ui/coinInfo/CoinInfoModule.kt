package app.khom.pavlo.crypto.ui.coinInfo

import app.khom.pavlo.crypto.di.PerActivity
import app.khom.pavlo.crypto.model.CoinsController
import app.khom.pavlo.crypto.model.GraphMaker
import app.khom.pavlo.crypto.model.HoldingsHandler
import app.khom.pavlo.crypto.model.network.NetworkRequests
import app.khom.pavlo.crypto.utils.Logger
import app.khom.pavlo.crypto.utils.ResourceProvider
import dagger.Module
import dagger.Provides


@Module
class CoinInfoModule {

    @Provides @PerActivity
    fun provideView(coinInfoActivity: CoinInfoActivity): ICoinInfo.View = coinInfoActivity

    @Provides @PerActivity
    fun providePresenter(view: ICoinInfo.View,
                         coinsController: CoinsController,
                         networkRequests: NetworkRequests,
                         graphMaker: GraphMaker,
                         holdingsHandler: HoldingsHandler,
                         resourceProvider: ResourceProvider,
                         logger: Logger): ICoinInfo.Presenter =
            CoinInfoPresenter(view, coinsController, networkRequests, graphMaker, holdingsHandler, resourceProvider, logger)

}