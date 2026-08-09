package app.khom.pavlo.crypto.model

import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.db.DBController
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import app.khom.pavlo.crypto.utils.Logger
import java.util.Locale


class CoinsController(private val dbController: DBController, db: CMDatabase, logger: Logger) {

    // Process-lifetime subscriptions owned by this application-scoped controller.
    private val subscriptions = CompositeDisposable()

    init {
        subscriptions.add(db.allCoinsDao().getAllCoins()
                .subscribeOn(Schedulers.io())
                .subscribe({
                    allInfoCoins = preferredCoinInfoBySymbol(it)
                    enrichSavedCoinsIfNeeded()
                }, { logger.logError("Observe all coin info: $it") }))
        subscriptions.add(db.coinsDao().getAllCoins()
                .subscribeOn(Schedulers.io())
                .subscribe({
                    allCoins = it.toList()
                    enrichSavedCoinsIfNeeded()
                }, { logger.logError("Observe saved coins: $it") }))
    }

    @Volatile private var allInfoCoins: Map<String, InfoCoin> = emptyMap()
    @Volatile private var allCoins: List<Coin> = emptyList()
    private var hasEnrichedCoins = false

    fun saveCoin(coin: Coin) {
        prepareCoinForSaving(coin)
        dbController.saveCoin(coin)
    }

    fun saveCoinAsync(coin: Coin): Completable {
        prepareCoinForSaving(coin)
        return dbController.saveCoinAsync(coin)
    }

    private fun prepareCoinForSaving(coin: Coin) {
        if (allInfoCoins.isNotEmpty()) addAdditionalInfoToCoin(coin)
    }

    fun addAdditionalInfoToCoin(coin: Coin) {
        addImageUrlToCoin(coin)
        addFullNameToCoin(coin)
    }

    private fun addImageUrlToCoin(coin: Coin) {
        allInfoCoins[coin.from.uppercase(Locale.US)]?.imageUrl
                ?.takeIf { it.isNotEmpty() }
                ?.let { coin.imgUrl = it }
    }

    private fun addFullNameToCoin(coin: Coin) {
        val info = allInfoCoins[coin.from.uppercase(Locale.US)] ?: return
        info.coinName.takeIf { it.isNotEmpty() }
                ?.let { coin.fullName = it }
                ?: info.fullName.takeIf { it.isNotEmpty() }
                        ?.let { coin.fullName = it }
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
        var metadataChanged = false
        val updated = allCoins.map {
            val previousImageUrl = it.imgUrl
            val previousFullName = it.fullName
            addAdditionalInfoToCoin(it)
            metadataChanged = metadataChanged ||
                    previousImageUrl != it.imgUrl || previousFullName != it.fullName
            it
        }
        hasEnrichedCoins = true
        if (metadataChanged) dbController.saveCoinsList(updated)
    }

    fun deleteCoinsAsync(coins: List<Coin>): Completable {
        val removedPairs = coins.mapTo(HashSet()) { it.from to it.to }
        return dbController.deleteCoinsAsync(coins)
                .doOnComplete {
                    allCoins = allCoins.filterNot { it.from to it.to in removedPairs }
                }
    }

    fun saveAllCoinsInfo(allCoins: List<InfoCoin>) {
        dbController.saveAllCoinsInfo(allCoins)
    }

    fun getCoin(from: String, to: String) = dbController.getCoin(from, to)

    fun saveTopCoinsList(list: List<TopCoinData>) {
        list.forEach { coin ->
            val info = allInfoCoins[coin.symbol.orEmpty().uppercase(Locale.US)]
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

    fun coinAlreadyAdded(coin: String) = allCoins.any { it.from.equals(coin, ignoreCase = true) }

    fun getStableCoinId(symbol: String): String =
        allInfoCoins[symbol.uppercase(Locale.US)]?.coinId?.takeIf { it.isNotBlank() } ?: symbol

    fun allInfoCoinsIsEmpty() = allInfoCoins.isEmpty()
}