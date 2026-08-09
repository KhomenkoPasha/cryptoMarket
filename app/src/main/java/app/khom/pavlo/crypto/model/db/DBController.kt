package app.khom.pavlo.crypto.model.db

import android.annotation.SuppressLint
import app.khom.pavlo.crypto.model.*
import app.khom.pavlo.crypto.utils.Logger
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers


class DBController(val db: CMDatabase, private val logger: Logger) {

    fun saveCoin(coin: Coin) {
        runWrite("save coin") { db.coinsDao().insert(coin) }
    }

    fun saveCoinAsync(coin: Coin): Completable =
            Completable.fromAction { db.coinsDao().insert(coin) }
                    .subscribeOn(Schedulers.io())

    fun saveCoinsList(list: List<Coin>) {
        runWrite("save coins") { db.coinsDao().insertList(list) }
    }

    fun getCoin(from: String, to: String) = db.coinsDao().getCoin(from, to)

    fun deleteCoinsAsync(coins: List<Coin>): Completable =
            Completable.fromAction { db.coinsDao().deleteCoins(coins) }
                    .subscribeOn(Schedulers.io())

    fun saveAllCoinsInfo(allCoins: List<InfoCoin>) {
        runWrite("save all coin info") { db.allCoinsDao().replaceAll(allCoins) }
    }

    fun saveTopCoinsList(list: List<TopCoinData>) {
        runWrite("save top coins") { db.topCoinsDao().replaceAll(list) }
    }

    @SuppressLint("CheckResult")
    private fun runWrite(operation: String, block: () -> Unit) {
        // Room writes are finite and application-scoped; retaining every completed Disposable
        // in a CompositeDisposable would itself make repeated writes accumulate in memory.
        Completable.fromAction(block)
            .subscribeOn(Schedulers.io())
            .subscribe({}, { logger.logError("Database $operation failed: $it") })
    }
}