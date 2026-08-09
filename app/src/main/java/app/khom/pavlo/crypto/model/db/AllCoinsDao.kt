package app.khom.pavlo.crypto.model.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.khom.pavlo.crypto.model.InfoCoin
import io.reactivex.rxjava3.core.Flowable


@Dao
interface AllCoinsDao {

    @Query("SELECT * FROM all_coins")
    fun getAllCoins(): Flowable<List<InfoCoin>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(coin: InfoCoin)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertList(list: List<InfoCoin>)

    @Query("DELETE FROM all_coins")
    fun clearAllCoins()

    @Transaction
    fun replaceAll(list: List<InfoCoin>) {
        clearAllCoins()
        insertList(list)
    }
}