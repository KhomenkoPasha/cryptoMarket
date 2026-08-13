package app.khom.pavlo.crypto.ui.coinInfo

import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.model.network.NetworkRequests
import app.khom.pavlo.crypto.model.rxbus.RxBus
import app.khom.pavlo.crypto.utils.*
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.SerialDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.math.BigDecimal
import javax.inject.Inject

class CoinInfoPresenter @Inject constructor(private val view: ICoinInfo.View,
                                            private val coinsController: CoinsController,
                                            private val networkRequests: NetworkRequests,
                                            private val graphMaker: GraphMaker,
                                            private val holdingsHandler: HoldingsHandler,
                                            private val resProvider: ResourceProvider,
                                            private val logger: Logger) : ICoinInfo.Presenter {

    private val disposable = CompositeDisposable()
    private val histoDisposable = SerialDisposable()
    private val statsDisposable = SerialDisposable()
    private var coin: Coin = Coin(from = "", to = "")
    private lateinit var from: String
    private lateinit var to: String

    init {
        disposable.add(histoDisposable)
        disposable.add(statsDisposable)
    }

    override fun onCreate(fromArg: String, toArg: String) {
        from = fromArg
        to = toArg
        getCoinByName(from, to)
    }

    private fun getCoinByName(from: String?, to: String?) {
        val requestedFrom = from?.takeIf { it.isNotBlank() } ?: return
        val requestedTo = to?.takeIf { it.isNotBlank() } ?: USD
        disposable.add(Single.fromCallable { coinsController.getCoin(requestedFrom, requestedTo) }
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
        requestMissing24hStats()
        view.setupSpinner()
    }

    private fun setCoinInfo() {
        view.setOpen(coin.open24h.orUnavailable())
        view.setHigh(coin.high24h.orUnavailable())
        view.setLow(coin.low24h.orUnavailable())
        view.setChange(coin.change24h.orUnavailable())
        view.setChangePct(coin.changePct24h.orUnavailable())
        view.setSupply(coin.supply.orUnavailable())
        view.setMarketCap(coin.mktCap.orUnavailable())
    }

    private fun requestMissing24hStats() {
        if (coin.open24h.isNotBlank() &&
            coin.high24h.isNotBlank() &&
            coin.low24h.isNotBlank() &&
            coin.change24h.isNotBlank()) {
            return
        }
        statsDisposable.set(
            networkRequests.getHistoPeriod(HOURS24, coin.from, coin.to)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::apply24hStats) { logger.logError("request24hStats $it") }
        )
    }

    private fun apply24hStats(candles: ArrayList<HistoData>) {
        val stats = calculateCoin24hStats(candles) ?: return
        if (coin.open24h.isBlank()) {
            coin.open24hRaw = stats.open.toFloat()
            coin.open24h = formatPrice(stats.open)
            view.setOpen(coin.open24h)
        }
        if (coin.high24h.isBlank()) {
            coin.high24hRaw = stats.high.toFloat()
            coin.high24h = formatPrice(stats.high)
            view.setHigh(coin.high24h)
        }
        if (coin.low24h.isBlank()) {
            coin.low24hRaw = stats.low.toFloat()
            coin.low24h = formatPrice(stats.low)
            view.setLow(coin.low24h)
        }
        if (coin.change24h.isBlank()) {
            coin.change24hRaw = stats.change.toFloat()
            coin.change24h = formatPrice(stats.change)
            view.setChange(coin.change24h)
        }
        if (coin.changePct24h.isBlank()) {
            coin.changePct24hRaw = stats.changePercent.toFloat()
            coin.changePct24h = PortfolioValueFormatter.percent(
                BigDecimal.valueOf(stats.changePercent)
            )
            view.setChangePct(coin.changePct24h)
        }
    }

    private fun formatPrice(value: Double): String =
        PortfolioValueFormatter.price(BigDecimal.valueOf(value))

    private fun String.orUnavailable(): String =
        ifBlank { resProvider.getString(R.string.value_unavailable) }


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
        histoDisposable.set(networkRequests.getHistoPeriod(period, coin.from, coin.to)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onHistoReceived(it, period) }, { onHistoError(it) }))
    }

    private fun onHistoError(error: Throwable) {
        logger.logError("requestHisto $error")
        view.disableGraphLoading()
        view.enableEmptyGraphText()
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