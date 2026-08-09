package app.khom.pavlo.crypto.model.backup

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupFileFormatTest {

    @Test
    fun createsCryptobackFileName() {
        val timestamp = LocalDateTime.of(2026, 8, 9, 20, 35)

        val fileName = createBackupFileName(timestamp)

        assertEquals("crypto-invest-pulse-2026-08-09_20-35.cryptoback", fileName)
        assertFalse(fileName.endsWith(".json"))
    }

    @Test
    fun acceptsCryptobackAndLegacyBackupMimeTypes() {
        assertEquals("application/x-cryptoback", BACKUP_FILE_MIME_TYPE)
        assertTrue(BACKUP_OPEN_MIME_TYPES.contains(BACKUP_FILE_MIME_TYPE))
        assertTrue(BACKUP_OPEN_MIME_TYPES.contains("application/json"))
    }
}