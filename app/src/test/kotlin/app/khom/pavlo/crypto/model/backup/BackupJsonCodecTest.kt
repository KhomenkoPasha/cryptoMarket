package app.khom.pavlo.crypto.model.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupJsonCodecTest {

    private val codec = BackupJsonCodec()

    @Test
    fun `backup round trips without losing transaction precision or notes`() {
        val document = AppBackupDocument(
            createdAtEpochMillis = 123456789L,
            appVersion = "2.0.1",
            favorites = listOf(
                FavoriteBackup(
                    from = "SOL",
                    to = "USD",
                    fullName = "Solana",
                    priceRaw = 176.12345f
                )
            ),
            transactions = listOf(
                TransactionBackup(
                    id = 42L,
                    from = "SOL",
                    to = "USD",
                    quantity = "0.1234567890123456789",
                    purchasePrice = "147.00000001",
                    dateEpochMillis = 123456789L,
                    coinName = "Solana"
                )
            ),
            preferences = listOf(
                PreferenceBackup("notes_coin_list_v1_SOL", "string", "my note"),
                PreferenceBackup("tracked", "long", "123456789")
            )
        )

        val decoded = codec.decode(codec.encode(document))

        assertEquals(document, decoded)
        assertEquals("0.1234567890123456789", decoded.transactions.single().quantity)
    }

    @Test
    fun `invalid and future backup formats are rejected`() {
        assertThrows(InvalidBackupException::class.java) {
            codec.decode("{\"format\":\"another-app\",\"schemaVersion\":1}")
        }
        assertThrows(InvalidBackupException::class.java) {
            codec.decode("{\"format\":\"$BACKUP_FORMAT\",\"schemaVersion\":999}")
        }
        assertThrows(InvalidBackupException::class.java) {
            codec.decode("not-json")
        }
    }

    @Test
    fun `duplicate favorites and malformed decimals are rejected`() {
        val duplicate = AppBackupDocument(
            favorites = listOf(
                FavoriteBackup(from = "btc", to = "usd"),
                FavoriteBackup(from = "BTC", to = "USD")
            )
        )
        assertThrows(InvalidBackupException::class.java) { codec.encode(duplicate) }

        val invalidTransaction = AppBackupDocument(
            transactions = listOf(
                TransactionBackup(from = "BTC", to = "USD", quantity = "NaN")
            )
        )
        assertThrows(InvalidBackupException::class.java) { codec.encode(invalidTransaction) }
    }

    @Test
    fun `all SharedPreferences value types round trip`() {
        val source = linkedMapOf<String, Any>(
            "string" to "note",
            "boolean" to true,
            "int" to 7,
            "long" to 8L,
            "float" to 1.25f,
            "set" to setOf("SOL", "BTC")
        )

        val restored = PreferenceBackupCodec.toValues(PreferenceBackupCodec.fromValues(source))

        assertEquals(source["string"], restored["string"])
        assertEquals(source["boolean"], restored["boolean"])
        assertEquals(source["int"], restored["int"])
        assertEquals(source["long"], restored["long"])
        assertEquals(source["float"], restored["float"])
        assertEquals(source["set"], restored["set"])
        assertTrue(restored.keys.containsAll(source.keys))
    }
}