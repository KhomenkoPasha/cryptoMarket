package app.khom.pavlo.crypto.ui.topCoins

import android.view.View
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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject


class TopCoinsPresenter @Inject constructor(private val view: ITopCoins.View,
                                            private val db: CMDatabase,
                                            private val networkRequests: NetworkRequests,
                                            private val coinsController: CoinsController,
                                            private val resProvider: ResourceProvider,
                                            private val pageController: PageController,
                                            private val toaster: Toaster,
                                            private val logger: Logger) : ITopCoins.Presenter {

    private val disposable = CompositeDisposable()
    private var coins: ArrayList<TopCoinData> = ArrayList()
    private var isRefreshing = false
    private var needToUpdate = false

    override fun onCreate(coins: ArrayList<TopCoinData>) {
        this.coins = coins
    }

    override fun onStart() {
        subscribeToObservables()
        updateTopCoins()
        updateAllCoins()
    }

    private fun subscribeToObservables() {
        disposable.add(db.topCoinsDao().getAllTopCoins()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onCoinsUpdated(it) }))
        disposable.add(db.allCoinsDao().getAllCoins()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onAllCoinsUpdated(it) }))
        disposable.add(RxBus.listen(MainCoinsListUpdatedEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onMainCoinsUpdated() })
        addOnPageChangedObservable()
    }

    private fun onCoinsUpdated(list: List<TopCoinData>) {
        if (list.isNotEmpty()) {
            coins.clear()
            coins.addAll(list)
            coins.sortBy { it.rank }
            view.updateRecyclerView()
        }
    }

    private fun onAllCoinsUpdated(list: List<InfoCoin>) {
        if (list.isNotEmpty()) {
            updateTopCoins()
        }
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
    }

    private fun updateTopCoins() {
        disposable.add(networkRequests.getTopCoins()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onTopCoinsReceived(it) },
                        { logger.logError("updateTopCoins $it") }))
    }

    private fun onTopCoinsReceived(coins: List<TopCoinData>) {
        if (coins.isNotEmpty()) {
            coinsController.saveTopCoinsList(coins)
            if (isRefreshing) {
                view.hideRefreshing()
                isRefreshing = false
            }
        }
    }

    private fun updateAllCoins() {
        if (coinsController.allInfoCoinsIsEmpty()) {
            disposable.add(networkRequests.getAllCoins()
                    .subscribe({ onAllCoinsReceived(it) },
                            { logger.logError("getAllCoinsInfo $it") }))
        } else {
            updateTopCoins()
        }
    }

    private fun onAllCoinsReceived(list: ArrayList<InfoCoin>) {
        if (list.isNotEmpty()) {
            coinsController.saveAllCoinsInfo(list)
        }
    }

    override fun onCoinClicked(coin: TopCoinData) {
        view.startCoinInfoActivity(coin.symbol)
    }

    override fun onSwipeUpdate() {
        isRefreshing = true
        updateTopCoins()
    }

    //todo get rid of View
    override fun onAddCoinClicked(coin: TopCoinData, itemView: View) {
        val loadingView = itemView.findViewById<View>(R.id.top_coin_add_loading)
        val iconView = itemView.findViewById<android.widget.ImageView>(R.id.top_coin_add_icon)
        loadingView.visibility = View.VISIBLE
        iconView.visibility = View.GONE
        val coinFrom = Coin(from = coin.symbol!!, to = USD)
        disposable.add(networkRequests.getPrice(createCoinsMapWithCurrencies(listOf(coinFrom)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onCoinAdded(it, itemView) }, { onError(itemView) }))
    }

    private fun onCoinAdded(list: ArrayList<Coin>, itemView: View) {
        if (list.isNotEmpty()) {
            coinsController.saveCoinsList(list)
            val iconView = itemView.findViewById<android.widget.ImageView>(R.id.top_coin_add_icon)
            iconView.setImageDrawable(resProvider.getDrawable(R.drawable.ic_done))
            toaster.toastShort(resProvider.getString(R.string.coin_added))
        } else {
            toaster.toastShort(resProvider.getString(R.string.error))
        }
        afterAdded(itemView)
    }

    private fun onError(itemView: View) {
        afterAdded(itemView)
        toaster.toastShort(resProvider.getString(R.string.error))
    }

    private fun afterAdded(itemView: View) {
        val loadingView = itemView.findViewById<View>(R.id.top_coin_add_loading)
        val iconView = itemView.findViewById<android.widget.ImageView>(R.id.top_coin_add_icon)
        loadingView.visibility = View.GONE
        iconView.visibility = View.VISIBLE
    }
}
