package app.khom.pavlo.crypto.model.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import app.khom.pavlo.crypto.model.*



@Database(entities = [
        Coin::class,
        InfoCoin::class,
        TopCoinData::class,
        HoldingData::class], version = 2)
abstract class CMDatabase : RoomDatabase() {

    abstract fun coinsDao(): CoinsDao

    abstract fun allCoinsDao(): AllCoinsDao

    abstract fun topCoinsDao(): TopCoinsDao

    abstract fun holdingsDao(): HoldingsDao
}