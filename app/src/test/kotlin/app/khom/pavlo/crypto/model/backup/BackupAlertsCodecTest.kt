package app.khom.pavlo.crypto.model.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupAlertsCodecTest {

    private val codec = BackupJsonCodec()

    private fun alert(
        id: Long = 1L,
        type: String = "PRICE_ABOVE",
        threshold: String = "90000"
    ) = AlertBackup(
        id = id,
        symbol = "BTC",
        coinName = "Bitcoin",
        type = type,
        threshold = threshold,
        currency = "EUR",
        enabled = false,
        createdAtEpochMillis = 1_700_000_000_000L,
        triggeredAtEpochMillis = 1_700_000_100_000L,
        triggeredValue = "€91,000.00"
    )

    @Test
    fun `alerts round trip through the backup file`() {
        val document = AppBackupDocument(alerts = listOf(alert(1L), alert(2L, "CHANGE_DOWN", "7.5")))

        val decoded = codec.decode(codec.encode(document))

        assertEquals(document, decoded)
        assertEquals(BACKUP_SCHEMA_VERSION, decoded.schemaVersion)
    }

    @Test
    fun `version one backups without alerts still decode`() {
        val legacy = """{"format":"$BACKUP_FORMAT","schemaVersion":1,"favorites":[],"transactions":[],"preferences":[]}"""

        val decoded = codec.decode(legacy)

        assertEquals(1, decoded.schemaVersion)
        assertTrue(decoded.alerts.isEmpty())
        assertTrue(decoded.schemaVersion < BACKUP_ALERTS_SINCE_VERSION)
    }

    @Test
    fun `invalid alerts are rejected`() {
        listOf(
            AppBackupDocument(alerts = listOf(alert(type = "UNKNOWN"))),
            AppBackupDocument(alerts = listOf(alert(threshold = "0"))),
            AppBackupDocument(alerts = listOf(alert(threshold = "-5"))),
            AppBackupDocument(alerts = listOf(alert(threshold = "abc"))),
            AppBackupDocument(alerts = listOf(alert(id = 3L), alert(id = 3L))),
            AppBackupDocument(alerts = listOf(alert().copy(symbol = " ")))
        ).forEach { document ->
            assertThrows(InvalidBackupException::class.java) { codec.encode(document) }
        }
    }
}
