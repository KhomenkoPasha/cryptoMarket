package app.khom.pavlo.crypto.coinInfo

import app.khom.pavlo.crypto.di.PerActivity
import app.khom.pavlo.crypto.models.CoinsController
import app.khom.pavlo.crypto.models.HoldingsHandler
import app.khom.pavlo.crypto.rest.NetworkRequests
import app.khom.pavlo.crypto.utills.Logger
import app.khom.pavlo.crypto.utills.ResourceProvider
import dagger.Provides
import dagger.Module


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