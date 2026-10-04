package app.khom.pavlo.crypto.model.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.khom.pavlo.crypto.model.Portfolio
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

@Dao
interface PortfoliosDao {

    @Query("SELECT * FROM portfolios ORDER BY sort_order ASC, id ASC")
    fun observeAll(): Flowable<List<Portfolio>>

    @Query("SELECT * FROM portfolios ORDER BY sort_order ASC, id ASC")
    fun getAllSync(): List<Portfolio>

    @Query("SELECT COUNT(*) FROM portfolios")
    fun countSync(): Int

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM portfolios")
    fun nextSortOrderSync(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSync(portfolio: Portfolio): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIfMissingSync(portfolio: Portfolio): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSync(portfolios: List<Portfolio>)

    @Query("DELETE FROM portfolios")
    fun clearPortfolios()

    @Transaction
    fun replaceAll(portfolios: List<Portfolio>) {
        clearPortfolios()
        insertAllSync(portfolios)
    }

    @Query("UPDATE portfolios SET name = :name WHERE id = :id")
    fun rename(id: Long, name: String): Completable

    @Query("DELETE FROM portfolios WHERE id = :id")
    fun deleteByIdSync(id: Long)
}
