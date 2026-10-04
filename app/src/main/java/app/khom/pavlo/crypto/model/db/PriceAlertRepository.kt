package app.khom.pavlo.crypto.model.db

import app.khom.pavlo.crypto.model.PriceAlert
import app.khom.pavlo.crypto.model.PriceAlertsChangeNotifier
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

class PriceAlertRepository(
    private val db: CMDatabase,
    private val changeNotifier: PriceAlertsChangeNotifier
) {

    fun observeAlerts(): Flowable<List<PriceAlert>> =
        db.priceAlertsDao().observeAll().distinctUntilChanged()

    fun addAlert(alert: PriceAlert): Single<Long> =
        db.priceAlertsDao().insert(alert).doOnSuccess { changeNotifier.onAlertsChanged() }

    fun setEnabled(id: Long, enabled: Boolean): Completable =
        db.priceAlertsDao().setEnabled(id, enabled).doOnComplete { changeNotifier.onAlertsChanged() }

    fun deleteAlert(id: Long): Completable =
        db.priceAlertsDao().deleteById(id).doOnComplete { changeNotifier.onAlertsChanged() }
}
