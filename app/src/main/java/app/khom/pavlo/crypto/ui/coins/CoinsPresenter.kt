package app.khom.pavlo.crypto.ui.coins

import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.network.NetworkRequests
import app.khom.pavlo.crypto.model.rxbus.*
import app.khom.pavlo.crypto.utils.PortfolioValueFormatter
import app.khom.pavlo.crypto.utils.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject
import java.math.BigDecimal


class CoinsPresenter @Inject constructor(private val view: ICoins.View,
                                         private val networkRequests: NetworkRequests,
                                         private val coinsController: CoinsController,
                                         private val db: CMDatabase,
                                         private val resProvider: ResourceProvider,
                                         private val pageController: PageController,
                                         private val multiSelector: MultiSelector,
                                         private val holdingsHandler: HoldingsHandler,
                                         private val favoritesChangeNotifier: FavoritesChangeNotifier,
                                         private val logger: Logger,
                                         private val toaster: Toaster,
                                         private val preferences: Preferences) : ICoins.Presenter {

    private val disposable = CompositeDisposable()
    private var coins: ArrayList<Coin> = ArrayList()
    private var holdings: ArrayList<HoldingData> = ArrayList()
    private var isRefreshing = false
    private var isFirstStart = true

    override fun onCreate(coins: ArrayList<Coin>) {
        this.coins = coins
    }

    override fun onStart() {
        view.setLoadingVisibility(coins.isEmpty())
        subscribeToObservables()
        if (coinsController.allInfoCoinsIsEmpty()) getAllCoinsInfo()
        if (coins.isNotEmpty()) updatePrices()
        updateHoldings()
    }

    private fun subscribeToObservables() {
        addCoinsChangesObservable()
        addHoldingsChangesObservable()
        setupRxBusEventsListeners()
        addOnPageChangedObservable()
    }

    private fun addCoinsChangesObservable() {
        disposable.add(db.coinsDao().getAllCoins()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onCoinsFromDbUpdates(it) }, {
                    view.setLoadingVisibility(false)
                    logger.logError("Observe coins: $it")
                }))
    }

    private fun onCoinsFromDbUpdates(list: List<Coin>) {
        view.setLoadingVisibility(false)
        if (list.isNotEmpty()) {
            val selectedCoins = alreadySelectedCoins()
            coins.clear()
            coins.addAll(list)
            if (selectedCoins.isNotEmpty()) {
                setSelectedCoins(selectedCoins)
            }
            sortCoinsBySelectedSortMethod()
            view.disableEmptyText()
            view.updateRecyclerView()
            if (isFirstStart) {
                isFirstStart = false
                updatePrices()
            }
            updateHoldings()
        } else {
            coins.clear()
            view.enableEmptyText()
            view.updateRecyclerView()
        }
    }

    private fun alreadySelectedCoins(): ArrayList<Coin> {
        val selectedCoins: ArrayList<Coin> = ArrayList()
        if (coins.isNotEmpty() && multiSelector.atLeastOneIsSelected) {
            coins.filter { it.selected }.forEach { selectedCoins.add(it) }
        }
        return selectedCoins
    }

    private fun setSelectedCoins(selectedCoins: ArrayList<Coin>) {
        selectedCoins.forEach { selectedCoin ->
            coins.find { it.from == selectedCoin.from }?.selected = true
        }
    }

    private fun addHoldingsChangesObservable() {
        disposable.add(db.holdingsDao().getAllHoldings()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onHoldingsUpdate(it) }, { logger.logError("Observe holdings: $it") }))
    }

    private fun onHoldingsUpdate(updatedHoldings: List<HoldingData>) {
        holdings.clear()
        holdingsHandler.setHoldingsSnapshot(updatedHoldings)
        if (updatedHoldings.isNotEmpty()) {
            holdings.addAll(updatedHoldings)
            updateHoldings()
        } else {
            view.disableTotalHoldings()
        }
    }

    private fun updateHoldings() {
        if (holdings.isNotEmpty()) {
            setTotalHoldingValue()
            setTotalHoldingsChangePercent()
            setTotalHoldingsChangeValue()
            view.enableTotalHoldings()
        }
    }

    private fun setTotalHoldingValue() {
        view.setTotalHoldingsValue(
            PortfolioValueFormatter.money(holdingsHandler.getTotalValueWithCurrentPrice())
        )
    }

    private fun setTotalHoldingsChangePercent() {
        val totalChangePercent = holdingsHandler.getTotalChangePercent()
        view.setTotalHoldingsChangePercent(PortfolioValueFormatter.percent(totalChangePercent))
        view.setTotalHoldingsChangePercentColor(getChangeColor(totalChangePercent))
    }

    private fun setTotalHoldingsChangeValue() {
        val totalChangeValue = holdingsHandler.getTotalChangeValue()
        view.setTotalHoldingsChangeValue(PortfolioValueFormatter.signedMoney(totalChangeValue))
        view.setTotalHoldingsChangeValueColor(getChangeColor(totalChangeValue))
        setAllTimeProfitLossString(totalChangeValue)
    }

    private fun setAllTimeProfitLossString(change: BigDecimal) {
        view.setAllTimeProfitLossString(getProfitLossText(change))
    }

    private fun getProfitLossText(change: BigDecimal) =
            if (change.signum() >= 0) resProvider.getString(R.string.profit) else resProvider.getString(R.string.loss)

    private fun setupRxBusEventsListeners() {
        disposable.add(RxBus.listen(OnDeleteCoinsMenuItemClickedEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onDeleteClicked() })
        disposable.add(RxBus.listen(CoinsSortMethodUpdated::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onSortMethodUpdated(it.sort) })
    }

    private fun onDeleteClicked() {
        val coinsToDelete = coins.filter { it.selected }
        if (coinsToDelete.isNotEmpty()) {
            disableSelected()
            removeFavorites(coinsToDelete)
        }
    }

    override fun onRemoveFavoriteClicked(coin: Coin) {
        view.showRemoveFavoriteConfirmation(coin)
    }

    override fun onRemoveFavoriteConfirmed(coin: Coin) {
        removeFavorites(listOf(coin))
    }

    private fun removeFavorites(favorites: List<Coin>) {
        disposable.add(
                coinsController.deleteCoinsAsync(favorites)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            favoritesChangeNotifier.onFavoritesChanged()
                            RxBus.publish(MainCoinsListUpdatedEvent())
                            toaster.toastShort(
                                    if (favorites.size > 1) resProvider.getString(R.string.coins_deleted)
                                    else resProvider.getString(R.string.coin_deleted)
                            )
                        }, {
                            logger.logError("Remove favorites: $it")
                            toaster.toastShort(resProvider.getString(R.string.error))
                        })
        )
    }

    private fun onSortMethodUpdated(sort: String?) {
        if (sort != null) preferences.sortBy = sort
        if (coins.isNotEmpty() && coins.size > 1) {
            sortCoinsBySelectedSortMethod()
        }
    }

    private fun sortCoinsBySelectedSortMethod() {
        when (preferences.sortBy) {
            CoinSort.NAME -> coins.sortBy { it.from }
            CoinSort.PRICE_ASCENDING -> coins.sortBy { it.priceRaw }
            CoinSort.PRICE_DESCENDING -> coins.sortByDescending { it.priceRaw }
            CoinSort.CHANGE_24H_ASCENDING -> coins.sortBy { it.changePct24hRaw }
            CoinSort.CHANGE_24H_DESCENDING -> coins.sortByDescending { it.changePct24hRaw }
        }
        view.updateRecyclerView()
    }

    private fun addOnPageChangedObservable() {
        disposable.add(pageController.getPageObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onPageChanged(it) })
    }

    private fun onPageChanged(position: Int) {
        if (position != COINS_FRAGMENT_PAGE_POSITION) {
            disableSelected()
        }
    }

    private fun disableSelected() {
        if (multiSelector.atLeastOneIsSelected) {
            coins.forEach {
                if (it.selected) {
                    it.selected = false
                }
            }
            view.updateRecyclerView()
            multiSelector.atLeastOneIsSelected = false
        }
    }

    private fun getAllCoinsInfo() {
        disposable.add(networkRequests.getAllCoins()
                .subscribe({ onAllCoinsReceived(it) },
                        { logger.logError("getAllCoinsInfo $it") }))
    }

    private fun onAllCoinsReceived(list: ArrayList<InfoCoin>) {
        if (list.isNotEmpty()) {
            coinsController.saveAllCoinsInfo(list)
            updatePrices()
        }
    }

    override fun onViewCreated() {

    }

    private fun updatePrices() {
        val queryMap = createCoinsMapWithCurrencies(coins)
        if (queryMap.isEmpty()) {
            afterRefreshing()
            return
        }
        RxBus.publish(CoinsLoadingEvent(true))
        disposable.add(networkRequests.getPrice(queryMap)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onPriceUpdated(it) }, { afterRefreshing() }))
    }

    private fun onPriceUpdated(list: ArrayList<Coin>) {
        if (list.isNotEmpty()) coinsController.saveCoinsList(filterList(list))
        afterRefreshing()
    }

    private fun filterList(coinsInfoList: ArrayList<Coin>): ArrayList<Coin> {
        val result: ArrayList<Coin> = ArrayList()
        coins.forEach { (from, to) ->
            val find = coinsInfoList.find { it.from == from && it.to == to }
            if (find != null) result.add(find)
        }
        return result
    }

    private fun afterRefreshing() {
        RxBus.publish(CoinsLoadingEvent(false))
        if (isRefreshing) {
            view.hideRefreshing()
            isRefreshing = false
            view.enableSwipeToRefresh()
        }
    }

    override fun onStop() {
        disposable.clear()
        disableSelected()
        view.setLoadingVisibility(false)
        RxBus.publish(CoinsLoadingEvent(false))
    }

    override fun onSwipeUpdate() {
        disableSelected()
        isRefreshing = true
        updatePrices()
    }

    override fun onCoinClicked(coin: Coin) {
        view.startCoinInfoActivity(coin.from, coin.to)
    }

    override fun onHoldingsClicked() {
        view.startHoldingsActivity()
    }

    override fun onAllocationsClicked() {
        view.startAllocationsActivity()
    }
}