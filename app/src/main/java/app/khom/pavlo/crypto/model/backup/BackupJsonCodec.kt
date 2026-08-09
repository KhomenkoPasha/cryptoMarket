package app.khom.pavlo.crypto.model.backup

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.math.BigDecimal
import java.util.Locale

class BackupJsonCodec(
    private val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()
) {

    fun encode(document: AppBackupDocument): String {
        validate(document)
        return gson.toJson(document)
    }

    fun decode(json: String): AppBackupDocument {
        if (json.isBlank()) throw InvalidBackupException("Backup file is empty")
        val root = try {
            JsonParser.parseString(json)
        } catch (error: RuntimeException) {
            throw InvalidBackupException("Backup is not valid JSON", error)
        }
        if (!root.isJsonObject) throw InvalidBackupException("Backup root must be a JSON object")
        val rootObject = root.asJsonObject
        listOf("favorites", "transactions", "preferences").forEach { field ->
            if (rootObject.has(field) && rootObject.get(field).isJsonNull) {
                throw InvalidBackupException("Backup field '$field' cannot be null")
            }
        }
        val document = try {
            gson.fromJson(rootObject, AppBackupDocument::class.java)
        } catch (error: RuntimeException) {
            throw InvalidBackupException("Backup structure is invalid", error)
        }
        validate(document)
        return document
    }

    private fun validate(document: AppBackupDocument) {
        if (document.format != BACKUP_FORMAT) {
            throw InvalidBackupException("Unsupported backup format")
        }
        if (document.schemaVersion !in 1..BACKUP_SCHEMA_VERSION) {
            throw InvalidBackupException("Unsupported backup version ${document.schemaVersion}")
        }
        if (document.favorites.size > MAX_FAVORITES ||
            document.transactions.size > MAX_TRANSACTIONS ||
            document.preferences.size > MAX_PREFERENCES
        ) {
            throw InvalidBackupException("Backup contains too many entries")
        }

        val favoriteKeys = HashSet<String>()
        document.favorites.forEach { favorite ->
            requireText(favorite.from, "Favorite symbol", MAX_SYMBOL_LENGTH)
            requireText(favorite.to, "Favorite currency", MAX_SYMBOL_LENGTH)
            requireLength(favorite.imageUrl, "Favorite image URL", MAX_TEXT_LENGTH)
            requireLength(favorite.fullName, "Favorite name", MAX_TEXT_LENGTH)
            if (!favorite.priceRaw.isFinite() ||
                !favorite.change24hRaw.isFinite() ||
                !favorite.changePct24hRaw.isFinite()
            ) {
                throw InvalidBackupException("Favorite contains an invalid numeric value")
            }
            val key = "${favorite.from.uppercase(Locale.US)}:${favorite.to.uppercase(Locale.US)}"
            if (!favoriteKeys.add(key)) throw InvalidBackupException("Backup contains duplicate favorites")
        }

        val transactionIds = HashSet<Long>()
        document.transactions.forEach { transaction ->
            if (transaction.id < 0L || !transactionIds.add(transaction.id)) {
                throw InvalidBackupException("Backup contains invalid transaction identifiers")
            }
            requireText(transaction.from, "Transaction symbol", MAX_SYMBOL_LENGTH)
            requireText(transaction.to, "Transaction currency", MAX_SYMBOL_LENGTH)
            requireLength(transaction.coinId, "Coin identifier", MAX_TEXT_LENGTH)
            requireLength(transaction.coinName, "Coin name", MAX_TEXT_LENGTH)
            requireLength(transaction.exchange, "Exchange", MAX_TEXT_LENGTH)
            requireDecimal(transaction.quantity, "Transaction quantity")
            requireDecimal(transaction.purchasePrice, "Transaction purchase price")
            if (transaction.dateEpochMillis < 0L) {
                throw InvalidBackupException("Transaction date is invalid")
            }
        }

        PreferenceBackupCodec.toValues(document.preferences)
    }

    private fun requireDecimal(value: String, label: String) {
        val decimal = try {
            BigDecimal(value)
        } catch (error: NumberFormatException) {
            throw InvalidBackupException("$label is invalid", error)
        }
        if (decimal.precision() > MAX_DECIMAL_PRECISION) {
            throw InvalidBackupException("$label is too large")
        }
    }

    private fun requireText(value: String, label: String, maxLength: Int) {
        if (value.isBlank()) throw InvalidBackupException("$label is missing")
        requireLength(value, label, maxLength)
    }

    private fun requireLength(value: String, label: String, maxLength: Int) {
        if (value.length > maxLength) throw InvalidBackupException("$label is too long")
    }

    private companion object {
        const val MAX_FAVORITES = 10_000
        const val MAX_TRANSACTIONS = 100_000
        const val MAX_PREFERENCES = 10_000
        const val MAX_SYMBOL_LENGTH = 32
        const val MAX_TEXT_LENGTH = 2_048
        const val MAX_DECIMAL_PRECISION = 100
    }
}

object PreferenceBackupCodec {
    private const val STRING = "string"
    private const val BOOLEAN = "boolean"
    private const val INT = "int"
    private const val LONG = "long"
    private const val FLOAT = "float"
    private const val STRING_SET = "string_set"

    fun fromValues(values: Map<String, *>): List<PreferenceBackup> = values
        .map { (key, value) ->
            when (value) {
                is String -> PreferenceBackup(key, STRING, value)
                is Boolean -> PreferenceBackup(key, BOOLEAN, value.toString())
                is Int -> PreferenceBackup(key, INT, value.toString())
                is Long -> PreferenceBackup(key, LONG, value.toString())
                is Float -> {
                    if (!value.isFinite()) throw InvalidBackupException("Preference '$key' is invalid")
                    PreferenceBackup(key, FLOAT, value.toString())
                }
                is Set<*> -> {
                    if (value.any { it !is String }) {
                        throw InvalidBackupException("Preference '$key' contains unsupported values")
                    }
                    PreferenceBackup(key, STRING_SET, values = value.filterIsInstance<String>().sorted())
                }
                else -> throw InvalidBackupException("Preference '$key' has an unsupported type")
            }
        }
        .sortedBy(PreferenceBackup::key)

    fun toValues(entries: List<PreferenceBackup>): Map<String, Any> {
        val values = LinkedHashMap<String, Any>(entries.size)
        entries.forEach { entry ->
            if (entry.key.isBlank() || entry.key.length > 512 || values.containsKey(entry.key)) {
                throw InvalidBackupException("Backup contains an invalid preference key")
            }
            val decoded: Any = try {
                when (entry.type) {
                    STRING -> entry.value
                    BOOLEAN -> when (entry.value) {
                        "true" -> true
                        "false" -> false
                        else -> throw InvalidBackupException("Preference '${entry.key}' is not boolean")
                    }
                    INT -> entry.value.toInt()
                    LONG -> entry.value.toLong()
                    FLOAT -> entry.value.toFloat().also {
                        if (!it.isFinite()) throw InvalidBackupException("Preference '${entry.key}' is invalid")
                    }
                    STRING_SET -> entry.values.toSet()
                    else -> throw InvalidBackupException("Unknown preference type '${entry.type}'")
                }
            } catch (error: NumberFormatException) {
                throw InvalidBackupException("Preference '${entry.key}' has an invalid value", error)
            }
            values[entry.key] = decoded
        }
        return values
    }
}