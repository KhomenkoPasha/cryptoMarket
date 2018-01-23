package app.khom.pavlo.crypto.models

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.reactivex.Flowable

@Dao
interface TopCoinsDao {

    @Query("SELECT * FROM top_coins")
    fun getAllTopCoins(): Flowable<List<TopCoinData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTopCoinsList(list: List<TopCoinData>)
}