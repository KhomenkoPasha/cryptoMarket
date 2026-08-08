package app.khom.pavlo.crypto.model.db

import androidx.room.*
import app.khom.pavlo.crypto.model.HoldingData
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable


@Dao
interface HoldingsDao {

    @Query("SELECT * FROM holdings")
    fun getAllHoldings(): Flowable<List<HoldingData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(holdingData: HoldingData): Completable

    @Delete
    fun deleteHolding(holdingData: HoldingData): Completable

    @Query("DELETE FROM holdings WHERE from_coin = :from AND to_currency = :to")
    fun deleteByPair(from: String, to: String): Completable
}
