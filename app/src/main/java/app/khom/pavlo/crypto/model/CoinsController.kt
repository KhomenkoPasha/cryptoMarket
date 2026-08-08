package app.khom.pavlo.crypto.model

import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.db.DBController
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import app.khom.pavlo.crypto.utils.Logger


class CoinsController(private val dbController: DBController, db: CMDatabase, logger: Logger) {

    // Process-lifetime subscriptions owned by this application-scoped controller.
    private val subscriptions = CompositeDisposable()

    init {
        subscriptions.add(db.allCoinsDao().getAllCoins()
                .subscribeOn(Schedulers.io())
                .subscribe({
                    allInfoCoins = it.associateBy(InfoCoin::name)
                    enrichSavedCoinsIfNeeded()
                }, { logger.logError("Observe all coin info: $it") }))
        subscriptions.add(db.coinsDao().getAllCoins()
                .subscribeOn(Schedulers.io())
                .subscribe({ allCoins = it.toList() }, { logger.logError("Observe saved coins: $it") }))
    }

    @Volatile private var allInfoCoins: Map<String, InfoCoin> = emptyMap()
    @Volatile private var allCoins: List<Coin> = emptyList()
    private var hasEnrichedCoins = false

    fun saveCoin(coin: Coin) {
        if (allInfoCoins.isNotEmpty()) {
            addAdditionalInfoToCoin(coin)
        }
        dbController.saveCoin(coin)
    }

    fun addAdditionalInfoToCoin(coin: Coin) {
        addImageUrlToCoin(coin)
        addFullNameToCoin(coin)
    }

    private fun addImageUrlToCoin(coin: Coin) {
        coin.imgUrl = allInfoCoins[coin.from]?.imageUrl ?: ""
    }

    private fun addFullNameToCoin(coin: Coin) {
        coin.fullName = allInfoCoins[coin.from]?.coinName ?: ""
    }

    fun saveCoinsList(list: List<Coin>) {
        if (list.isNotEmpty() && allInfoCoins.isNotEmpty()) {
            list.forEach {
                addAdditionalInfoToCoin(it)
            }
        }
        dbController.saveCoinsList(list)
    }

    @Synchronized
    private fun enrichSavedCoinsIfNeeded() {
        if (hasEnrichedCoins) return
        if (allInfoCoins.isEmpty() || allCoins.isEmpty()) return
        val updated = allCoins.map {
            if (it.imgUrl.isEmpty() || it.fullName.isEmpty()) {
                addAdditionalInfoToCoin(it)
            }
            it
        }
        hasEnrichedCoins = true
        dbController.saveCoinsList(updated)
    }

    fun deleteCoin(coin: Coin) = dbController.deleteCoin(coin)

    fun deleteCoins(coins: List<Coin>) = dbController.deleteCoins(coins)

    fun saveAllCoinsInfo(allCoins: List<InfoCoin>) {
        dbController.saveAllCoinsInfo(allCoins)
    }

    fun getCoin(from: String, to: String) = dbController.getCoin(from, to)

    fun saveTopCoinsList(list: List<TopCoinData>) {
        list.forEach { coin ->
            val info = allInfoCoins[coin.symbol]
            if (info != null && info.imageUrl.isNotEmpty()) {
                coin.imgUrl = info.imageUrl
            }
        }
        dbController.saveTopCoinsList(list)
    }

    fun coinIsAdded(coin: TopCoinData): Boolean {
        if (allCoins.isNotEmpty()) {
            return allCoins.find { it.from == coin.symbol && it.to == USD } != null
        }
        return false
    }

    fun coinAlreadyAdded(coin: String) = allCoins.find { it.from == coin } != null

    fun allInfoCoinsIsEmpty() = allInfoCoins.isEmpty()
}
