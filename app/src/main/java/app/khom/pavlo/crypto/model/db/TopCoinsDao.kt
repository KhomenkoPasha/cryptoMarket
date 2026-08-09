package app.khom.pavlo.crypto.model.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.khom.pavlo.crypto.model.TopCoinData
import io.reactivex.rxjava3.core.Flowable


@Dao
interface TopCoinsDao {

    @Query("SELECT * FROM top_coins")
    fun getAllTopCoins(): Flowable<List<TopCoinData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTopCoinsList(list: List<TopCoinData>)

    @Query("DELETE FROM top_coins")
    fun clearTopCoins()

    @Transaction
    fun replaceAll(list: List<TopCoinData>) {
        clearTopCoins()
        insertTopCoinsList(list)
    }
}