package app.khom.pavlo.crypto.ui.addCoin

import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.network.NetworkRequests
import app.khom.pavlo.crypto.utils.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AddCoinPresenter @Inject constructor(private val view: IAddCoin.View,
                                           private val coinsController: CoinsController,
                                           private val networkRequests: NetworkRequests,
                                           private val resProvider: ResourceProvider,
                                           private val db: CMDatabase,
                                           private val toaster: Toaster,
                                           private val logger: Logger): IAddCoin.Presenter {

    private val disposable = CompositeDisposable()
    @Volatile private var allCoins: List<InfoCoin> = emptyList()
    private var coins: ArrayList<Coin> = ArrayList()
    private lateinit var matches: ArrayList<InfoCoin>
    private lateinit var fromTextObservable: Observable<CharSequence>

    override fun onCreate( matches: ArrayList<InfoCoin>) {
        this.matches = matches
    }

    override fun onStart() {
        addAllInfoCoinsChangesObservable()
        addCoinsChangesObservable()
        if (::fromTextObservable.isInitialized) subscribeToFromText()
    }

    private fun addAllInfoCoinsChangesObservable() {
        disposable.add(db.allCoinsDao().getAllCoins()
                .subscribeOn(Schedulers.io())
                .subscribe({ onAllCoinsUpdates(it) }, { logger.logError("Observe all coins: $it") }))
    }

    private fun onAllCoinsUpdates(coinsList: List<InfoCoin>) {
        allCoins = preferredCoinInfoBySymbol(coinsList).values.toList()
    }

    private fun addCoinsChangesObservable() {
        disposable.add(db.coinsDao().getAllCoins()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onCoinsFromDbUpdates(it) }, { logger.logError("Observe saved coins: $it") }))
    }

    private fun onCoinsFromDbUpdates(list: List<Coin>) {
        coins.clear()
        coins.addAll(list)
    }

    override fun onStop() {
        disposable.clear()
    }

    override fun observeFromText(observable: Observable<CharSequence>) {
        fromTextObservable = observable
    }

    private fun subscribeToFromText() {
        disposable.add(fromTextObservable
                .debounce(250, TimeUnit.MILLISECONDS)
                .map(CharSequence::toString)
                .distinctUntilChanged()
                .observeOn(Schedulers.computation())
                .map(::findMatches)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { filtered -> onMatchesArrived(filtered) },
                        { logger.logError("Observe add coin text: $it") }
                ))
    }

    private fun findMatches(text: String): List<InfoCoin> {
        if (text.isBlank()) return emptyList()
        return allCoins.asSequence()
                .filter { it.coinName.contains(text, true) || it.name.contains(text, true) }
                .filterNot { coinsController.coinAlreadyAdded(it.name) }
                .take(MAX_MATCHES)
                .toList()
    }

    private fun onMatchesArrived(filtered: List<InfoCoin>) {
        view.enableMatchesCount()
        matches.clear()
        matches.addAll(filtered)
        updateCoinsList()
    }

    private fun updateCoinsList() {
        view.setMatchesResultSize(if (matches.size > 0) matches.size.toString() else "0")
        view.updateRecyclerView()
    }

    override fun onFromItemClicked(coin: InfoCoin) {
        view.disableMatchesCount()
        matches.clear()
        matches.add(coin)
        view.updateRecyclerView()
        view.hideKeyboard()
        requestCoinInfo(Coin(from = coin.name, to = USD))
    }

    private fun requestCoinInfo(coin: Coin) {
        view.enableLoadingLayout()
        disposable.add(networkRequests.getPrice(createCoinsMapWithCurrencies(listOf(coin)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onPriceUpdated(it) }, { coinNotFound() }))
    }

    private fun onPriceUpdated(list: ArrayList<Coin>) {
        if (list.isNotEmpty()) {
            coinsController.saveCoinsList(list)
            coinSuccessfullyAdded()
        } else {
            coinNotFound()
        }
    }

    private fun coinNotFound() {
        view.disableLoadingLayout()
        toaster.toastShort(resProvider.getString(R.string.coin_not_found))
    }

    private fun coinSuccessfullyAdded() {
        toaster.toastShort(resProvider.getString(R.string.coin_added))
        view.finishActivity()
    }

    private companion object {
        const val MAX_MATCHES = 100
    }
}