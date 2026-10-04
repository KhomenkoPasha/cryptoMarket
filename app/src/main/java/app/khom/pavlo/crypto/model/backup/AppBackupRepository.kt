package app.khom.pavlo.crypto.model.backup

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import app.khom.pavlo.crypto.BuildConfig
import app.khom.pavlo.crypto.model.Coin
import app.khom.pavlo.crypto.model.CurrencyManager
import app.khom.pavlo.crypto.model.DEFAULT_PORTFOLIO_ID
import app.khom.pavlo.crypto.model.Portfolio
import app.khom.pavlo.crypto.model.PriceAlert
import app.khom.pavlo.crypto.model.HoldingData
import app.khom.pavlo.crypto.model.Preferences
import app.khom.pavlo.crypto.model.db.CMDatabase
import io.reactivex.rxjava3.core.Single
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.math.BigDecimal

class AppBackupRepository(
    private val context: Context,
    private val database: CMDatabase,
    private val changeNotifier: BackupChangeNotifier,
    private val codec: BackupJsonCodec = BackupJsonCodec(),
    private val preferencesName: String = Preferences.PREFS_NAME
) {

    fun exportTo(uri: Uri): Single<BackupResult> = Single.fromCallable {
        val document = createDocument()
        val json = codec.encode(document)
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException("Could not open backup destination")
        OutputStreamWriter(stream, Charsets.UTF_8).buffered().use { writer ->
            writer.write(json)
        }
        BackupResult(document.favorites.size, document.transactions.size)
    }

    fun restoreFrom(uri: Uri): Single<BackupResult> = Single.fromCallable {
        restoreFromJson(readBackup(uri))
    }

    fun createBackupJson(): String = codec.encode(createDocument())

    fun restoreFromJson(json: String): BackupResult {
        val document = codec.decode(json)
        restore(document)
        return BackupResult(document.favorites.size, document.transactions.size)
    }

    private fun createDocument(): AppBackupDocument {
        var favorites: List<Coin> = emptyList()
        var transactions: List<HoldingData> = emptyList()
        var alerts: List<PriceAlert> = emptyList()
        var portfolios: List<Portfolio> = emptyList()
        database.runInTransaction {
            favorites = database.coinsDao().getAllCoinsSync()
            transactions = database.holdingsDao().getAllHoldingsSync()
            alerts = database.priceAlertsDao().getAllSync()
            portfolios = database.portfoliosDao().getAllSync()
        }
        val preferenceValues = preferences().all
        return AppBackupDocument(
            createdAtEpochMillis = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            favorites = favorites.map { it.toBackup() },
            transactions = transactions.map { it.toBackup() },
            alerts = alerts.map { it.toBackup() },
            portfolios = portfolios.map { it.toBackup() },
            preferences = PreferenceBackupCodec.fromValues(preferenceValues)
        )
    }

    private fun restore(document: AppBackupDocument) {
        val importedPreferences = PreferenceBackupCodec.toValues(document.preferences)
        val originalPreferences = PreferenceBackupCodec.toValues(
            PreferenceBackupCodec.fromValues(preferences().all)
        )
        if (!replacePreferences(importedPreferences)) {
            throw IllegalStateException("Could not restore application preferences")
        }
        try {
            database.runInTransaction {
                database.coinsDao().replaceAll(document.favorites.map { it.toCoin() })
                if (document.schemaVersion >= BACKUP_PORTFOLIOS_SINCE_VERSION) {
                    database.portfoliosDao().replaceAll(document.portfolios.map { it.toPortfolio() })
                }
                database.holdingsDao().replaceAll(document.transactions.map { it.toHolding() })
                // Every transaction needs its portfolio; the built-in one always exists.
                database.portfoliosDao().insertIfMissingSync(
                    Portfolio(id = DEFAULT_PORTFOLIO_ID, createdAt = System.currentTimeMillis(), sortOrder = 0)
                )
                if (document.schemaVersion >= BACKUP_ALERTS_SINCE_VERSION) {
                    database.priceAlertsDao().replaceAll(document.alerts.map { it.toAlert() })
                }
            }
        } catch (error: Throwable) {
            if (!replacePreferences(originalPreferences)) {
                error.addSuppressed(IllegalStateException("Could not roll back application preferences"))
            }
            throw error
        }
        // Restored settings may change the display currency, so refresh the in-memory copy right away.
        runCatching { CurrencyManager.load(Preferences(context)) }
        runCatching { changeNotifier.onBackupRestored() }
    }

    private fun replacePreferences(values: Map<String, Any>): Boolean {
        val editor = preferences().edit().clear()
        values.forEach { (key, value) -> editor.putValue(key, value) }
        return editor.commit()
    }

    private fun preferences(): SharedPreferences =
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    private fun readBackup(uri: Uri): String {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not open backup file")
        return InputStreamReader(stream, Charsets.UTF_8).buffered().use { reader ->
            val output = StringBuilder()
            val buffer = CharArray(8_192)
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                output.append(buffer, 0, count)
                if (output.length > MAX_BACKUP_CHARACTERS) {
                    throw InvalidBackupException("Backup file is too large")
                }
            }
            output.toString()
        }
    }

    private fun SharedPreferences.Editor.putValue(key: String, value: Any) {
        when (value) {
            is String -> putString(key, value)
            is Boolean -> putBoolean(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
            else -> throw InvalidBackupException("Unsupported preference value")
        }
    }

    private fun Coin.toBackup() = FavoriteBackup(
        from = from,
        to = to,
        imageUrl = imgUrl,
        fullName = fullName,
        fromSymbol = fromSymbol,
        toSymbol = toSymbol,
        market = market,
        price = price,
        priceRaw = priceRaw,
        change24h = change24h,
        change24hRaw = change24hRaw,
        changePct24h = changePct24h,
        changePct24hRaw = changePct24hRaw
    )

    private fun FavoriteBackup.toCoin() = Coin(
        from = from,
        to = to,
        imgUrl = imageUrl,
        fullName = fullName,
        selected = false,
        fromSymbol = fromSymbol,
        toSymbol = toSymbol,
        market = market,
        price = price,
        priceRaw = priceRaw,
        change24h = change24h,
        change24hRaw = change24hRaw,
        changePct24h = changePct24h,
        changePct24hRaw = changePct24hRaw
    )

    private fun HoldingData.toBackup() = TransactionBackup(
        id = id,
        from = from,
        to = to,
        quantity = quantity.toPlainString(),
        purchasePrice = price.toPlainString(),
        dateEpochMillis = date,
        coinId = coinId,
        coinName = coinName,
        exchange = exchange,
        type = tradeType.name,
        fee = fee.toPlainString(),
        portfolioId = portfolioId
    )

    private fun Portfolio.toBackup() = PortfolioBackup(
        id = id,
        name = name,
        createdAtEpochMillis = createdAt,
        sortOrder = sortOrder
    )

    private fun PortfolioBackup.toPortfolio() = Portfolio(
        id = id,
        name = name,
        createdAt = createdAtEpochMillis,
        sortOrder = sortOrder
    )

    private fun PriceAlert.toBackup() = AlertBackup(
        id = id,
        symbol = symbol,
        coinName = coinName,
        type = type,
        threshold = threshold.toPlainString(),
        currency = currency,
        enabled = enabled,
        createdAtEpochMillis = createdAt,
        triggeredAtEpochMillis = triggeredAt,
        triggeredValue = triggeredValue
    )

    private fun AlertBackup.toAlert() = PriceAlert(
        id = id,
        symbol = symbol,
        coinName = coinName,
        type = type,
        threshold = BigDecimal(threshold),
        currency = currency,
        enabled = enabled,
        createdAt = createdAtEpochMillis,
        triggeredAt = triggeredAtEpochMillis,
        triggeredValue = triggeredValue
    )

    private fun TransactionBackup.toHolding() = HoldingData(
        id = id,
        from = from,
        to = to,
        quantity = BigDecimal(quantity),
        price = BigDecimal(purchasePrice),
        date = dateEpochMillis,
        coinId = coinId,
        coinName = coinName,
        exchange = exchange,
        type = type,
        fee = BigDecimal(fee),
        portfolioId = portfolioId
    )

    private companion object {
        const val MAX_BACKUP_CHARACTERS = 5_000_000
    }
}