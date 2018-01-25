package app.khom.pavlo.crypto.model.db

import android.arch.persistence.room.*
import app.khom.pavlo.crypto.model.HoldingData
import io.reactivex.Flowable


@Dao
interface HoldingsDao {

    @Query("SELECT * FROM holdings")
    fun getAllHoldings(): Flowable<List<HoldingData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(holdingData: HoldingData)

    @Delete
    fun deleteHolding(holdingData: HoldingData)
}