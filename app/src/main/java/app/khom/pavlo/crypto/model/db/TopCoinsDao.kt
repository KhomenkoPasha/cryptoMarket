package app.khom.pavlo.crypto.model.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.khom.pavlo.crypto.model.TopCoinData
import io.reactivex.Flowable


@Dao
interface TopCoinsDao {

    @Query("SELECT * FROM top_coins")
    fun getAllTopCoins(): Flowable<List<TopCoinData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTopCoinsList(list: List<TopCoinData>)
}
