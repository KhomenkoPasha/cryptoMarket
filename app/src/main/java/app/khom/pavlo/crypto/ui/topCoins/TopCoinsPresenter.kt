package app.khom.pavlo.crypto.ui.topCoins

import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.rxbus.MainCoinsListUpdatedEvent
import app.khom.pavlo.crypto.model.rxbus.RxBus
import app.khom.pavlo.crypto.model.network.NetworkRequests
import app.khom.pavlo.crypto.utils.Logger
import app.khom.pavlo.crypto.utils.ResourceProvider
import app.khom.pavlo.crypto.utils.Toaster
import app.khom.pavlo.crypto.utils.createCoinsMapWithCurrencies
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject


class TopCoinsPresenter @Inject constructor(private val view: ITopCoins.View,
                                            private val db: CMDatabase,
                                            private val networkRequests: NetworkRequests,
                                            private val coinsController: CoinsController,
                                            private val resProvider: ResourceProvider,
                                            private val pageController: PageController,
                                            private val preferences: Preferences,
                                            private val toaster: Toaster,
                                            private val logger: Logger) : ITopCoins.Presenter {

    private val disposable = CompositeDisposable()
    private var coins: ArrayList<TopCoinData> = ArrayList()
    private var isRefreshing = false
    private var needToUpdate = false
    private var topCoinsRequestInFlight = false
    private var initialCacheHandled = false
    private val addingSymbols = mutableSetOf<String>()

    override fun onCreate(coins: ArrayList<TopCoinData>) {
        this.coins = coins
    }

    override fun onStart() {
        initialCacheHandled = false
        view.setLoadingVisibility(true)
        subscribeToObservables()
    }

    private fun subscribeToObservables() {
        disposable.add(db.topCoinsDao().getAllTopCoins()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onCoinsUpdated(it) }, { logger.logError("Observe top coins: $it") }))
        disposable.add(RxBus.listen(MainCoinsListUpdatedEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onMainCoinsUpdated() })
        addOnPageChangedObservable()
    }

    private fun onCoinsUpdated(list: List<TopCoinData>) {
        coins.clear()
        coins.addAll(list)
        coins.sortBy { it.rank }
        view.updateRecyclerView()

        if (list.isNotEmpty()) {
            view.showContent()
            view.setLoadingVisibility(false)
        }

        if (!initialCacheHandled) {
            initialCacheHandled = true
            if (list.isEmpty() || topCoinsCacheIsStale()) {
                updateTopCoins()
            } else {
                view.setLoadingVisibility(false)
            }
        }
    }

    private fun topCoinsCacheIsStale(): Boolean {
        val lastUpdated = preferences.topCoinsLastUpdated
        return lastUpdated <= 0L || System.currentTimeMillis() - lastUpdated >= TOP_COINS_CACHE_TTL_MS
    }

    private fun onMainCoinsUpdated() {
        needToUpdate = true
    }

    private fun addOnPageChangedObservable() {
        disposable.add(pageController.getPageObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onPageChanged(it) })
    }

    private fun onPageChanged(position: Int) {
        if (position == TOP_COINS_FRAGMENT_PAGE_POSITION && needToUpdate) {
            view.updateRecyclerView()
            needToUpdate = false
        }
    }

    override fun onStop() {
        disposable.clear()
        topCoinsRequestInFlight = false
        addingSymbols.toList().forEach { view.setCoinAdding(it, false) }
        addingSymbols.clear()
        view.setLoadingVisibility(false)
        if (isRefreshing) {
            view.hideRefreshing()
            isRefreshing = false
        }
    }

    private fun updateTopCoins() {
        if (topCoinsRequestInFlight) return
        topCoinsRequestInFlight = true
        if (!isRefreshing) view.setLoadingVisibility(true)
        disposable.add(networkRequests.getTopCoins()
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { topCoinsRequestInFlight = false }
                .subscribe({ onTopCoinsReceived(it) },
                        { onTopCoinsError(it) }))
    }

    private fun onTopCoinsError(error: Throwable) {
        logger.logError("updateTopCoins $error")
        view.setLoadingVisibility(false)
        if (coins.isEmpty()) {
            view.showLoadError()
        } else {
            toaster.toastShort(resProvider.getString(R.string.top_coins_load_error))
        }
        if (isRefreshing) {
            view.hideRefreshing()
            isRefreshing = false
        }
    }

    private fun onTopCoinsReceived(coins: List<TopCoinData>) {
        view.setLoadingVisibility(false)
        if (coins.isNotEmpty()) {
            this.coins.clear()
            this.coins.addAll(coins.sortedBy { it.rank })
            view.updateRecyclerView()
            view.showContent()
            preferences.topCoinsLastUpdated = System.currentTimeMillis()
            coinsController.saveTopCoinsList(coins)
        } else if (this.coins.isEmpty()) {
            view.showLoadError()
        }
        if (isRefreshing) {
            view.hideRefreshing()
            isRefreshing = false
        }
    }

    override fun onCoinClicked(coin: TopCoinData) {
        val symbol = coin.symbol?.takeIf { it.isNotBlank() }
        if (symbol != null) {
            view.startCoinInfoActivity(symbol)
        } else {
            toaster.toastShort(resProvider.getString(R.string.error))
        }
    }

    override fun onSwipeUpdate() {
        isRefreshing = true
        updateTopCoins()
    }

    override fun onRetryClicked() {
        view.setLoadingVisibility(true)
        updateTopCoins()
    }

    override fun onAddCoinClicked(coin: TopCoinData) {
        val symbol = coin.symbol?.takeIf { it.isNotBlank() }
        if (symbol == null) {
            toaster.toastShort(resProvider.getString(R.string.error))
            return
        }
        if (!addingSymbols.add(symbol)) return
        view.setCoinAdding(symbol, true)
        val coinFrom = Coin(from = symbol, to = USD)
        disposable.add(networkRequests.getPrice(createCoinsMapWithCurrencies(listOf(coinFrom)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onCoinAdded(symbol, it) }, { onAddError(symbol) }))
    }

    private fun onCoinAdded(symbol: String, list: ArrayList<Coin>) {
        if (list.isNotEmpty()) {
            coinsController.saveCoinsList(list)
            addingSymbols.remove(symbol)
            view.setCoinAdded(symbol)
            toaster.toastShort(resProvider.getString(R.string.coin_added))
        } else {
            finishAdding(symbol)
            toaster.toastShort(resProvider.getString(R.string.error))
        }
    }

    private fun onAddError(symbol: String) {
        finishAdding(symbol)
        toaster.toastShort(resProvider.getString(R.string.error))
    }

    private fun finishAdding(symbol: String) {
        addingSymbols.remove(symbol)
        view.setCoinAdding(symbol, false)
    }

    private companion object {
        const val TOP_COINS_CACHE_TTL_MS = 60L * 60L * 1000L
    }
}
