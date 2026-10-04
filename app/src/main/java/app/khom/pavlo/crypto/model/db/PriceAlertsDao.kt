package app.khom.pavlo.crypto.model.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.khom.pavlo.crypto.model.PriceAlert
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

@Dao
interface PriceAlertsDao {

    @Query("SELECT * FROM price_alerts ORDER BY enabled DESC, created_at DESC, id DESC")
    fun observeAll(): Flowable<List<PriceAlert>>

    @Query("SELECT * FROM price_alerts ORDER BY id ASC")
    fun getAllSync(): List<PriceAlert>

    @Query("SELECT * FROM price_alerts WHERE enabled = 1")
    fun getActiveSync(): List<PriceAlert>

    @Query("SELECT COUNT(*) FROM price_alerts WHERE enabled = 1")
    fun activeCountSync(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(alert: PriceAlert): Single<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSync(alerts: List<PriceAlert>)

    @Query("DELETE FROM price_alerts")
    fun clearAlerts()

    @Transaction
    fun replaceAll(alerts: List<PriceAlert>) {
        clearAlerts()
        insertAllSync(alerts)
    }

    @Query("DELETE FROM price_alerts WHERE id = :id")
    fun deleteById(id: Long): Completable

    @Query("UPDATE price_alerts SET enabled = :enabled, triggered_at = 0, triggered_value = '' WHERE id = :id")
    fun setEnabled(id: Long, enabled: Boolean): Completable

    @Query("UPDATE price_alerts SET enabled = 0, triggered_at = :triggeredAt, triggered_value = :value WHERE id = :id")
    fun markTriggeredSync(id: Long, triggeredAt: Long, value: String)
}
