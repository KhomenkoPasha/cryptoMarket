package app.khom.pavlo.crypto.ui.coinInfo

import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.model.network.NetworkRequests
import app.khom.pavlo.crypto.model.rxbus.RxBus
import app.khom.pavlo.crypto.utils.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class CoinInfoPresenter @Inject constructor(private val view: ICoinInfo.View,
                                            private val coinsController: CoinsController,
                                            private val networkRequests: NetworkRequests,
                                            private val graphMaker: GraphMaker,
                                            private val holdingsHandler: HoldingsHandler,
                                            private val resProvider: ResourceProvider,
                                            private val logger: Logger) : ICoinInfo.Presenter {

    private val disposable = CompositeDisposable()
    private var coin: Coin = Coin(from = "", to = "")
    private lateinit var from: String
    private lateinit var to: String

    override fun onCreate(fromArg: String, toArg: String) {
        from = fromArg
        to = toArg
        getCoinByName(from, to)
    }

    private fun getCoinByName(from: String?, to: String?) {
        disposable.add(Single.fromCallable { coinsController.getCoin(from!!, to!!) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onCoinArrived(it) }, { onFindCoinError(it) }))
    }

    private fun onCoinArrived(coin: Coin) {
        this.coin = coin
        view.setTitle(coin.fullName)
        view.setLogo(coin.imgUrl)
        view.setMainPrice(coin.price)
        setCoinInfo()
        view.setupSpinner()
    }

    private fun setCoinInfo() {
        view.setOpen(coin.open24h)
        view.setHigh(coin.high24h)
        view.setLow(coin.low24h)
        view.setChange(coin.change24h)
        view.setChangePct(coin.changePct24h)
        view.setSupply(coin.supply)
        view.setMarketCap(coin.mktCap)
    }


    private fun onFindCoinError(throwable: Throwable) {
        logger.logDebug("getCoinByName error " + throwable.toString())
        requestCoinInfo(Coin(from = this.from, to = this.to))
    }

    private fun requestCoinInfo(coin: Coin) {
        disposable.add(networkRequests.getPrice(createCoinsMapWithCurrencies(listOf(coin)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onPriceUpdated(it) }, { logger.logError("requestCoinInfo $it") }))
    }

    private fun onPriceUpdated(list: ArrayList<Coin>) {
        if (list.isNotEmpty()) {
            val arrivedCoin = list[0]
            coinsController.addAdditionalInfoToCoin(arrivedCoin)
            onCoinArrived(arrivedCoin)
        }
    }

    override fun onSpinnerItemClicked(position: Int) {
        view.enableGraphLoading()
        requestHisto(when (position) {
            0 -> HOUR
            1 -> HOURS12
            2 -> HOURS24
            3 -> DAYS3
            4 -> WEEK
            5 -> MONTH
            6 -> MONTHS3
            7 -> MONTHS6
            8 -> YEAR
            else -> MONTH
        })
    }

    private fun requestHisto(period: String) {
        disposable.add(networkRequests.getHistoPeriod(period, coin.from, coin.to)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onHistoReceived(it, period) }, { view.disableGraphLoading() }))
    }

    private fun onHistoReceived(histoList: ArrayList<HistoData>, period: String) {
        view.disableGraphLoading()
        if (histoList.isNotEmpty()) {
            view.drawChart(graphMaker.makeChart(histoList, period))
            view.disableEmptyGraphText()
        } else {
            view.enableEmptyGraphText()
        }
    }

    override fun onDestroy() {
        disposable.clear()
    }

}