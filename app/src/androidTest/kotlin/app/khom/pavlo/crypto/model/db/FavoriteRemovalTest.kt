package app.khom.pavlo.crypto.model.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.HoldingData
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class FavoriteRemovalTest {

    private lateinit var database: CMDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CMDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun removingFavoriteKeepsInvestmentTransaction() {
        val favorite = Coin(from = "SOL", to = "USD", fullName = "Solana")
        val transaction = HoldingData(
            from = "SOL",
            to = "USD",
            quantity = BigDecimal("2.5"),
            price = BigDecimal("120"),
            date = 1_700_000_000L,
            coinId = "5426",
            coinName = "Solana"
        )
        database.coinsDao().insert(favorite)
        database.holdingsDao().insert(transaction).blockingAwait()

        database.coinsDao().deleteCoin(favorite)

        assertTrue(database.coinsDao().getAllCoinsSync().isEmpty())
        val savedTransactions = database.holdingsDao().getAllHoldingsSync()
        assertEquals(1, savedTransactions.size)
        assertEquals("SOL", savedTransactions.single().from)
    }
}