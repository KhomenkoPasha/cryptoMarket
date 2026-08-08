package app.khom.pavlo.crypto.model.db

import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.HoldingData
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

class PortfolioRepository(private val db: CMDatabase) {

    fun observeHoldings(): Flowable<List<HoldingData>> =
        db.holdingsDao().getAllHoldings().distinctUntilChanged()

    fun addHolding(holding: HoldingData): Completable = db.holdingsDao().insert(holding)

    fun deleteHolding(holding: HoldingData): Completable = db.holdingsDao().deleteHolding(holding)

    fun deleteHoldingsForCoins(coins: List<Coin>): Completable =
        Completable.concat(
            coins.distinctBy { it.from to it.to }
                .map { db.holdingsDao().deleteByPair(it.from, it.to) }
        )
}
