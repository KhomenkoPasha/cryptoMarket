package app.khom.pavlo.crypto.model.db

import app.khom.pavlo.crypto.model.Coin
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

class CoinsRepository(private val db: CMDatabase) {

    fun observeCoins(): Flowable<List<Coin>> = db.coinsDao().getAllCoins().distinctUntilChanged()

    fun getCoins(): Single<List<Coin>> = Single.fromCallable { db.coinsDao().getAllCoinsSync() }

    fun getCoin(from: String, to: String): Single<Coin> =
        Single.fromCallable { db.coinsDao().getCoin(from, to) }
}
