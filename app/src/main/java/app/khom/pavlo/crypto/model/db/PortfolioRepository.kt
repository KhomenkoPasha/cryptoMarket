package app.khom.pavlo.crypto.model.db

import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.PortfolioChangeNotifier
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

class PortfolioRepository(
    private val db: CMDatabase,
    private val changeNotifier: PortfolioChangeNotifier
) {

    fun observeHoldings(): Flowable<List<HoldingData>> =
        db.holdingsDao().getAllHoldings().distinctUntilChanged()

    fun addHolding(holding: HoldingData): Completable =
        db.holdingsDao().insert(holding).notifyWidget()

    fun getHolding(id: Long): Single<HoldingData> = db.holdingsDao().getById(id)

    fun updateHolding(holding: HoldingData): Completable =
        db.holdingsDao().update(holding).notifyWidget()

    fun deleteHolding(holding: HoldingData): Completable =
        db.holdingsDao().deleteHolding(holding).notifyWidget()

    private fun Completable.notifyWidget(): Completable = doOnComplete {
        changeNotifier.onPortfolioChanged()
    }
}