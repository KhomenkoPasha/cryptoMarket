package app.khom.pavlo.crypto.model.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.db.CMDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class AppBackupRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferencesName = "backup_repository_test"
    private lateinit var database: CMDatabase
    private lateinit var repository: AppBackupRepository
    private val notificationCount = AtomicInteger()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, CMDatabase::class.java).build()
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().clear().commit()
        repository = AppBackupRepository(
            context = context,
            database = database,
            changeNotifier = BackupChangeNotifier { notificationCount.incrementAndGet() },
            preferencesName = preferencesName
        )
    }

    @After
    fun tearDown() {
        database.close()
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun exportClearAndRestore_preservesPersonalData() {
        val favorite = Coin(
            from = "SOL",
            to = "USD",
            imgUrl = "https://example.test/sol.png",
            fullName = "Solana",
            price = "\$ 176.12",
            priceRaw = 176.12f
        )
        val transaction = HoldingData(
            from = "SOL",
            to = "USD",
            quantity = BigDecimal("0.1234567890123456789"),
            price = BigDecimal("147.00000001"),
            date = 1_786_000_000_000L,
            coinId = "sol-solana",
            coinName = "Solana",
            exchange = "Kraken"
        )
        database.coinsDao().insert(favorite)
        database.holdingsDao().insertAllSync(listOf(transaction))
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString("notes_coin_list_v1_SOL", "important note")
            .putBoolean("custom_flag", true)
            .commit()

        val json = repository.createBackupJson()
        database.coinsDao().clearCoins()
        database.holdingsDao().clearHoldings()
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().clear().commit()
        assertFalse(database.coinsDao().getAllCoinsSync().isNotEmpty())

        val result = repository.restoreFromJson(json)

        assertEquals(1, result.favoriteCount)
        assertEquals(1, result.transactionCount)
        assertEquals(favorite.copy(selected = false), database.coinsDao().getAllCoinsSync().single())
        val restoredTransaction = database.holdingsDao().getAllHoldingsSync().single()
        assertEquals(transaction.quantity, restoredTransaction.quantity)
        assertEquals(transaction.price, restoredTransaction.price)
        assertEquals(transaction.coinId, restoredTransaction.coinId)
        assertEquals(transaction.exchange, restoredTransaction.exchange)
        val restoredPreferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        assertEquals("important note", restoredPreferences.getString("notes_coin_list_v1_SOL", null))
        assertEquals(true, restoredPreferences.getBoolean("custom_flag", false))
        assertEquals(1, notificationCount.get())
    }
}