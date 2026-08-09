package app.khom.pavlo.crypto.model.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `coins` (
                `from_name` TEXT NOT NULL,
                `to_name` TEXT NOT NULL,
                `imgUrl` TEXT NOT NULL,
                `fullName` TEXT NOT NULL,
                `selected` INTEGER NOT NULL,
                `fromSymbol` TEXT NOT NULL,
                `toSymbol` TEXT NOT NULL,
                `market` TEXT NOT NULL,
                `price` TEXT NOT NULL,
                `priceRaw` REAL NOT NULL,
                `lastUpdate` TEXT NOT NULL,
                `lastUpdateRaw` REAL NOT NULL,
                `lastVolume` TEXT NOT NULL,
                `lastVolumeRaw` REAL NOT NULL,
                `lastVolumeTo` TEXT NOT NULL,
                `lastVolumeToRaw` REAL NOT NULL,
                `lastTradeId` REAL NOT NULL,
                `volume24h` TEXT NOT NULL,
                `volume24hRaw` REAL NOT NULL,
                `volume24hTo` TEXT NOT NULL,
                `volume24hToRaw` REAL NOT NULL,
                `open24h` TEXT NOT NULL,
                `open24hRaw` REAL NOT NULL,
                `high24h` TEXT NOT NULL,
                `high24hRaw` REAL NOT NULL,
                `low24h` TEXT NOT NULL,
                `low24hRaw` REAL NOT NULL,
                `lastMarket` TEXT NOT NULL,
                `change24h` TEXT NOT NULL,
                `change24hRaw` REAL NOT NULL,
                `changePct24h` TEXT NOT NULL,
                `changePct24hRaw` REAL NOT NULL,
                `supply` TEXT NOT NULL,
                `supplyRaw` REAL NOT NULL,
                `mktCap` TEXT NOT NULL,
                `mktCapRaw` REAL NOT NULL,
                `flags` TEXT NOT NULL,
                PRIMARY KEY(`from_name`, `to_name`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT OR REPLACE INTO `coins` (
                `from_name`, `to_name`, `imgUrl`, `fullName`, `selected`,
                `fromSymbol`, `toSymbol`, `market`, `price`, `priceRaw`,
                `lastUpdate`, `lastUpdateRaw`, `lastVolume`, `lastVolumeRaw`,
                `lastVolumeTo`, `lastVolumeToRaw`, `lastTradeId`, `volume24h`,
                `volume24hRaw`, `volume24hTo`, `volume24hToRaw`, `open24h`,
                `open24hRaw`, `high24h`, `high24hRaw`, `low24h`, `low24hRaw`,
                `lastMarket`, `change24h`, `change24hRaw`, `changePct24h`,
                `changePct24hRaw`, `supply`, `supplyRaw`, `mktCap`, `mktCapRaw`, `flags`
            )
            SELECT
                `from_name`, `to_name`, `imgUrl`, `fullName`, `selected`,
                `FROMSYMBOL`, `TOSYMBOL`, `MARKET`, `PRICE`, 0,
                `LASTUPDATE`, 0, `LASTVOLUME`, 0, `LASTVOLUMETO`, 0,
                `LASTTRADEID`, `VOLUME24HOUR`, 0, `VOLUME24HOURTO`, 0,
                `OPEN24HOUR`, 0, `HIGH24HOUR`, 0, `LOW24HOUR`, 0,
                `LASTMARKET`, `CHANGE24HOUR`, 0, `CHANGEPCT24HOUR`, 0,
                `SUPPLY`, 0, `MKTCAP`, 0, ''
            FROM `display_coins`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `display_coins`")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        recreateAllCoins(db, nullableColumns = setOf("fullyPremined"))
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        recreateAllCoins(
            db,
            nullableColumns = setOf(
                "fullyPremined",
                "totalCoinSupply",
                "preMinedValue",
                "totalCoinsFreeFloat"
            )
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `holdings_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `from_coin` TEXT NOT NULL,
                `to_currency` TEXT NOT NULL,
                `quantity` TEXT NOT NULL,
                `price` TEXT NOT NULL,
                `transaction_date` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `holdings_new` (`from_coin`, `to_currency`, `quantity`, `price`, `transaction_date`)
            SELECT `from_coin`, `to_currency`, CAST(`quantity` AS TEXT), CAST(`price` AS TEXT), `transaction_date`
            FROM `holdings`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `holdings`")
        db.execSQL("ALTER TABLE `holdings_new` RENAME TO `holdings`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_holdings_from_coin_to_currency` " +
                "ON `holdings` (`from_coin`, `to_currency`)"
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `holdings` ADD COLUMN `coin_id` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `holdings` ADD COLUMN `coin_name` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `holdings` ADD COLUMN `exchange` TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE `holdings` SET `coin_id` = `from_coin` WHERE `coin_id` = ''")
    }
}

val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6
)

private fun recreateAllCoins(db: SupportSQLiteDatabase, nullableColumns: Set<String>) {
    fun type(column: String) = if (column in nullableColumns) "TEXT" else "TEXT NOT NULL"

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `all_coins_new` (
            `coinId` TEXT NOT NULL,
            `url` TEXT NOT NULL,
            `imageUrl` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `coinName` TEXT NOT NULL,
            `fullName` TEXT NOT NULL,
            `algorithm` TEXT NOT NULL,
            `proofType` TEXT NOT NULL,
            `fullyPremined` ${type("fullyPremined")},
            `totalCoinSupply` ${type("totalCoinSupply")},
            `preMinedValue` ${type("preMinedValue")},
            `totalCoinsFreeFloat` ${type("totalCoinsFreeFloat")},
            `sortOrder` TEXT NOT NULL,
            PRIMARY KEY(`coinId`)
        )
        """.trimIndent()
    )
    db.execSQL(
        """
        INSERT INTO `all_coins_new` (
            `coinId`, `url`, `imageUrl`, `name`, `coinName`, `fullName`,
            `algorithm`, `proofType`, `fullyPremined`, `totalCoinSupply`,
            `preMinedValue`, `totalCoinsFreeFloat`, `sortOrder`
        )
        SELECT
            `coinId`, `url`, `imageUrl`, `name`, `coinName`, `fullName`,
            `algorithm`, `proofType`, `fullyPremined`, `totalCoinSupply`,
            `preMinedValue`, `totalCoinsFreeFloat`, `sortOrder`
        FROM `all_coins`
        """.trimIndent()
    )
    db.execSQL("DROP TABLE `all_coins`")
    db.execSQL("ALTER TABLE `all_coins_new` RENAME TO `all_coins`")
}