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

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
