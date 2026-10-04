package app.khom.pavlo.crypto.model.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CMDatabase::class.java
    )

    @Test
    fun migration4To5PreservesHoldingAndConvertsDecimalsToText() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                """
                INSERT INTO holdings(from_coin, to_currency, quantity, price, transaction_date)
                VALUES ('BTC', 'USD', 0.12345678, 20123.45, 1700000000)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5).use { db ->
            db.query("SELECT from_coin, quantity, price FROM holdings").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("BTC", cursor.getString(0))
                assertEquals("0.12345678", cursor.getString(1))
                assertEquals("20123.45", cursor.getString(2))
            }
        }
    }

    @Test
    fun migration5To6PreservesHoldingAndAddsPortfolioMetadata() {
        helper.createDatabase(TEST_DB_V6, 5).apply {
            execSQL(
                """
                INSERT INTO holdings(from_coin, to_currency, quantity, price, transaction_date)
                VALUES ('BTC', 'USD', '0.25', '20000', 1700000000)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB_V6, 6, true, MIGRATION_5_6).use { db ->
            db.query(
                "SELECT from_coin, quantity, price, coin_id, coin_name, exchange FROM holdings"
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("BTC", cursor.getString(0))
                assertEquals("0.25", cursor.getString(1))
                assertEquals("20000", cursor.getString(2))
                assertEquals("BTC", cursor.getString(3))
                assertEquals("", cursor.getString(4))
                assertEquals("", cursor.getString(5))
            }
        }
    }

    @Test
    fun migration6To7KeepsHoldingsAndCreatesPriceAlertsTable() {
        helper.createDatabase(TEST_DB_V7, 6).apply {
            execSQL(
                """
                INSERT INTO holdings(from_coin, to_currency, quantity, price, transaction_date, coin_id, coin_name, exchange)
                VALUES ('ETH', 'USD', '2', '1500', 1700000000, 'ETH', 'Ethereum', 'Binance')
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB_V7, 7, true, MIGRATION_6_7).use { db ->
            db.query("SELECT from_coin, price FROM holdings").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("ETH", cursor.getString(0))
                assertEquals("1500", cursor.getString(1))
            }
            db.execSQL(
                """
                INSERT INTO price_alerts(symbol, coin_name, type, threshold, currency, enabled, created_at, triggered_at, triggered_value)
                VALUES ('BTC', 'Bitcoin', 'PRICE_ABOVE', '90000', 'USD', 1, 1700000000, 0, '')
                """.trimIndent()
            )
            db.query("SELECT symbol, type, threshold, enabled FROM price_alerts").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("BTC", cursor.getString(0))
                assertEquals("PRICE_ABOVE", cursor.getString(1))
                assertEquals("90000", cursor.getString(2))
                assertEquals(1, cursor.getInt(3))
            }
        }
    }

    @Test
    fun migration7To8DefaultsExistingTransactionsToBuysWithoutFees() {
        helper.createDatabase(TEST_DB_V8, 7).apply {
            execSQL(
                """
                INSERT INTO holdings(from_coin, to_currency, quantity, price, transaction_date, coin_id, coin_name, exchange)
                VALUES ('BTC', 'USD', '1.5', '40000', 1700000000, 'BTC', 'Bitcoin', '')
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB_V8, 8, true, MIGRATION_7_8).use { db ->
            db.query("SELECT from_coin, quantity, price, type, fee FROM holdings").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("BTC", cursor.getString(0))
                assertEquals("1.5", cursor.getString(1))
                assertEquals("40000", cursor.getString(2))
                assertEquals("BUY", cursor.getString(3))
                assertEquals("0", cursor.getString(4))
            }
        }
    }

    @Test
    fun migration8To9CreatesTheDefaultPortfolioAndAssignsExistingTransactions() {
        helper.createDatabase(TEST_DB_V9, 8).apply {
            execSQL(
                """
                INSERT INTO holdings(from_coin, to_currency, quantity, price, transaction_date, coin_id, coin_name, exchange, type, fee)
                VALUES ('BTC', 'USD', '1', '50000', 1700000000, 'BTC', 'Bitcoin', '', 'SELL', '5')
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB_V9, 9, true, MIGRATION_8_9).use { db ->
            db.query("SELECT id, name FROM portfolios").use { cursor ->
                assertEquals(1, cursor.count)
                assertEquals(true, cursor.moveToFirst())
                assertEquals(1L, cursor.getLong(0))
                assertEquals("", cursor.getString(1))
            }
            db.query("SELECT type, fee, portfolio_id FROM holdings").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("SELL", cursor.getString(0))
                assertEquals("5", cursor.getString(1))
                assertEquals(1L, cursor.getLong(2))
            }
        }
    }

    @Test
    fun migration9To10AddsTheSnapshotsTableAndKeepsPortfolios() {
        helper.createDatabase(TEST_DB_V10, 9).apply {
            execSQL("INSERT INTO portfolios(id, name, created_at, sort_order) VALUES (2, 'Cold wallet', 5, 1)")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB_V10, 10, true, MIGRATION_9_10).use { db ->
            db.query("SELECT name FROM portfolios WHERE id = 2").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("Cold wallet", cursor.getString(0))
            }
            db.execSQL(
                "INSERT INTO portfolio_snapshots(portfolio_id, day, value, invested, net_flow) " +
                    "VALUES (2, 20000, '1500.5', '1000', '0')"
            )
            db.query("SELECT value FROM portfolio_snapshots WHERE portfolio_id = 2 AND day = 20000").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("1500.5", cursor.getString(0))
            }
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
        const val TEST_DB_V10 = "migration-test-v10"
        const val TEST_DB_V9 = "migration-test-v9"
        const val TEST_DB_V6 = "migration-test-v6"
        const val TEST_DB_V7 = "migration-test-v7"
        const val TEST_DB_V8 = "migration-test-v8"
    }
}