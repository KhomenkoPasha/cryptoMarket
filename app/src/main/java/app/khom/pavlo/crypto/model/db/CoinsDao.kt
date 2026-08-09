package app.khom.pavlo.crypto.model.db

import androidx.room.*
import app.khom.pavlo.crypto.model.Coin
import io.reactivex.rxjava3.core.Flowable

@Dao
interface CoinsDao {

    @Query("SELECT * FROM coins")
    fun getAllCoins(): Flowable<List<Coin>>

    @Query("SELECT * FROM coins ORDER BY from_name ASC")
    fun getAllCoinsSync(): List<Coin>

    @Query("SELECT * FROM coins WHERE from_name = :from AND to_name = :to LIMIT 1")
    fun getCoin(from: String, to: String): Coin

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(coin: Coin)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertList(list: List<Coin>)

    @Query("DELETE FROM coins")
    fun clearCoins()

    @Transaction
    fun replaceAll(list: List<Coin>) {
        clearCoins()
        insertList(list)
    }

    @Delete
    fun deleteCoin(coin: Coin)

    @Delete
    fun deleteCoins(coins: List<Coin>)
}