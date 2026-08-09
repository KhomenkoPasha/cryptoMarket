package app.khom.pavlo.crypto.model.db

import androidx.room.*
import app.khom.pavlo.crypto.model.HoldingData
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single


@Dao
interface HoldingsDao {

    @Query("SELECT * FROM holdings ORDER BY transaction_date DESC, id DESC")
    fun getAllHoldings(): Flowable<List<HoldingData>>

    @Query("SELECT * FROM holdings ORDER BY transaction_date DESC, id DESC")
    fun getAllHoldingsSync(): List<HoldingData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(holdingData: HoldingData): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSync(holdings: List<HoldingData>)

    @Query("DELETE FROM holdings")
    fun clearHoldings()

    @Transaction
    fun replaceAll(holdings: List<HoldingData>) {
        clearHoldings()
        insertAllSync(holdings)
    }

    @Query("SELECT * FROM holdings WHERE id = :id LIMIT 1")
    fun getById(id: Long): Single<HoldingData>

    @Update
    fun update(holdingData: HoldingData): Completable

    @Delete
    fun deleteHolding(holdingData: HoldingData): Completable

    @Query("DELETE FROM holdings WHERE from_coin = :from AND to_currency = :to")
    fun deleteByPair(from: String, to: String): Completable
}