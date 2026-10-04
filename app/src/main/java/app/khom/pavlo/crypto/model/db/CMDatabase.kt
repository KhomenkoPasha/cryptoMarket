package app.khom.pavlo.crypto.model.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.khom.pavlo.crypto.model.*



@Database(entities = [
        Coin::class,
        InfoCoin::class,
        TopCoinData::class,
        HoldingData::class,
        PriceAlert::class,
        Portfolio::class,
        PortfolioSnapshot::class], version = 10)
@TypeConverters(DecimalConverters::class)
abstract class CMDatabase : RoomDatabase() {

    abstract fun coinsDao(): CoinsDao

    abstract fun allCoinsDao(): AllCoinsDao

    abstract fun topCoinsDao(): TopCoinsDao

    abstract fun holdingsDao(): HoldingsDao

    abstract fun priceAlertsDao(): PriceAlertsDao

    abstract fun portfoliosDao(): PortfoliosDao

    abstract fun portfolioSnapshotsDao(): PortfolioSnapshotsDao
}