package app.khom.pavlo.crypto.model

import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.db.DBController
import io.reactivex.schedulers.Schedulers


class CoinsController(private val dbController: DBController, db: CMDatabase) {

    init {
        db.allCoinsDao().getAllCoins()
                .subscribeOn(Schedulers.io())
                .subscribe({
                    allInfoCoins = it
                    enrichSavedCoinsIfNeeded()
                })
        db.coinsDao().getAllCoins()
                .subscribeOn(Schedulers.io())
                .subscribe({ allCoins = it })
    }

    private var allInfoCoins: List<InfoCoin> = mutableListOf()
    private var allCoins: List<Coin> = mutableListOf()
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
        coin.imgUrl = allInfoCoins.find { it.name == coin.from }?.imageUrl ?: ""
    }

    private fun addFullNameToCoin(coin: Coin) {
        coin.fullName = allInfoCoins.find { it.name == coin.from }?.coinName ?: ""
    }

    fun saveCoinsList(list: List<Coin>) {
        if (list.isNotEmpty() && allInfoCoins.isNotEmpty()) {
            list.forEach {
                addAdditionalInfoToCoin(it)
            }
        }
        dbController.saveCoinsList(list)
    }

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
            val info = allInfoCoins.find { it.name == coin.symbol }
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
