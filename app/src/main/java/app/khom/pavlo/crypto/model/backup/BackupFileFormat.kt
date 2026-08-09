package app.khom.pavlo.crypto.model.backup

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val BACKUP_FILE_MIME_TYPE = "application/x-cryptoback"

val BACKUP_OPEN_MIME_TYPES = arrayOf(
    BACKUP_FILE_MIME_TYPE,
    "application/octet-stream",
    "application/json",
    "text/plain"
)

fun createBackupFileName(timestamp: LocalDateTime = LocalDateTime.now()): String =
    "crypto-invest-pulse-${timestamp.format(BACKUP_FILE_TIME_FORMAT)}.cryptoback"

private val BACKUP_FILE_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")