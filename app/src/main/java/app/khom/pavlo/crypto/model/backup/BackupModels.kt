package app.khom.pavlo.crypto.model.backup

const val BACKUP_FORMAT = "crypto-invest-pulse-backup"
const val BACKUP_SCHEMA_VERSION = 3

/** First schema version that carries price alerts; older files leave existing alerts untouched on restore. */
const val BACKUP_ALERTS_SINCE_VERSION = 2

/** First schema version that carries portfolios; older files leave portfolios untouched on restore. */
const val BACKUP_PORTFOLIOS_SINCE_VERSION = 3

data class AppBackupDocument(
    val format: String = BACKUP_FORMAT,
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val createdAtEpochMillis: Long = 0L,
    val appVersion: String = "",
    val favorites: List<FavoriteBackup> = emptyList(),
    val transactions: List<TransactionBackup> = emptyList(),
    val alerts: List<AlertBackup> = emptyList(),
    val portfolios: List<PortfolioBackup> = emptyList(),
    val preferences: List<PreferenceBackup> = emptyList()
)

data class FavoriteBackup(
    val from: String = "",
    val to: String = "USD",
    val imageUrl: String = "",
    val fullName: String = "",
    val fromSymbol: String = "",
    val toSymbol: String = "",
    val market: String = "",
    val price: String = "",
    val priceRaw: Float = 0f,
    val change24h: String = "",
    val change24hRaw: Float = 0f,
    val changePct24h: String = "",
    val changePct24hRaw: Float = 0f
)

data class TransactionBackup(
    val id: Long = 0L,
    val from: String = "",
    val to: String = "USD",
    val quantity: String = "0",
    val purchasePrice: String = "0",
    val dateEpochMillis: Long = 0L,
    val coinId: String = "",
    val coinName: String = "",
    val exchange: String = "",
    val type: String = "BUY",
    val fee: String = "0",
    val portfolioId: Long = 1L
)

data class PortfolioBackup(
    val id: Long = 0L,
    val name: String = "",
    val createdAtEpochMillis: Long = 0L,
    val sortOrder: Int = 0
)

data class AlertBackup(
    val id: Long = 0L,
    val symbol: String = "",
    val coinName: String = "",
    val type: String = "",
    val threshold: String = "0",
    val currency: String = "USD",
    val enabled: Boolean = true,
    val createdAtEpochMillis: Long = 0L,
    val triggeredAtEpochMillis: Long = 0L,
    val triggeredValue: String = ""
)

data class PreferenceBackup(
    val key: String = "",
    val type: String = "",
    val value: String = "",
    val values: List<String> = emptyList()
)

data class BackupResult(
    val favoriteCount: Int,
    val transactionCount: Int
)

class InvalidBackupException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)