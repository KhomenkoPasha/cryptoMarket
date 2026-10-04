package app.khom.pavlo.crypto.model.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.khom.pavlo.crypto.model.PortfolioSnapshot
import io.reactivex.rxjava3.core.Flowable

@Dao
interface PortfolioSnapshotsDao {

    @Query("SELECT * FROM portfolio_snapshots ORDER BY day ASC")
    fun observeAll(): Flowable<List<PortfolioSnapshot>>

    @Query("SELECT * FROM portfolio_snapshots WHERE portfolio_id = :portfolioId ORDER BY day ASC")
    fun getForPortfolioSync(portfolioId: Long): List<PortfolioSnapshot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSync(snapshots: List<PortfolioSnapshot>)

    @Query("DELETE FROM portfolio_snapshots WHERE portfolio_id = :portfolioId")
    fun deleteForPortfolioSync(portfolioId: Long)

    @Query("DELETE FROM portfolio_snapshots")
    fun deleteAllSync()

    @Query("DELETE FROM portfolio_snapshots WHERE portfolio_id NOT IN (SELECT id FROM portfolios)")
    fun deleteOrphansSync()

    @Transaction
    fun replaceForPortfolio(portfolioId: Long, snapshots: List<PortfolioSnapshot>) {
        deleteForPortfolioSync(portfolioId)
        insertAllSync(snapshots)
    }
}
