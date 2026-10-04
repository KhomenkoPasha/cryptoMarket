package app.khom.pavlo.crypto.model

import android.content.Context
import java.util.Locale

class Preferences(context: Context) {

    companion object {
        const val CHART_STYLE_LINE = "line"
        const val CHART_STYLE_CANDLES = "candles"
        val PREFS_NAME = "com.rmnivnv.cryptomoon"
        val SEARCH_HASH_TAG = "search_hash_tag"
        val SEARCH_HASH_TAG_DEFAULT = ""
        val SORT_BY = "coins_sort_by"
        val SORT_BY_DEFAULT = CoinSort.NAME
        val SELECTED_LANGUAGE = "selected_language"
        val SELECTED_LANGUAGE_DEFAULT = ""
        private const val THEME_MODE = "theme_mode"
        private const val CURRENCY_CODE = "currency_code"
        private const val CHART_STYLE = "chart_style"
        private const val ACTIVE_PORTFOLIO_ID = "active_portfolio_id"
        private const val HISTORY_DIRTY = "portfolio_history_dirty"
        private const val APP_LOCK_ENABLED = "app_lock_enabled"
        private const val FX_RATE_PREFIX = "fx_rate_"
        private const val FX_RATES_UPDATED_AT = "fx_rates_updated_at"
        private const val INSIGHTS_NOTE_PREFIX = "insights_note_"
        private const val INSIGHTS_TRACKED_DATE_PREFIX = "insights_tracked_date_"
        private const val INSIGHTS_TRACKED_PRICE_PREFIX = "insights_tracked_price_"
        private const val INSIGHTS_NEWS_NOTES = "insights_news_notes"
        private const val NOTES_NEWS_LIST = "notes_news_list_v1"
        private const val NOTES_COIN_LIST_PREFIX = "notes_coin_list_v1_"
        private const val TOP_COINS_LAST_UPDATED = "top_coins_last_updated"
        private const val LEGACY_SEARCH_HASH_TAG_DEFAULT = "cryptocurrency"
        private const val NEWS_SEARCH_DEFAULT_MIGRATED = "news_search_default_migrated_v1"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        migrateLegacyNewsSearchDefault()
    }

    var searchHashTag: String
        get() = prefs.getString(SEARCH_HASH_TAG, SEARCH_HASH_TAG_DEFAULT) ?: SEARCH_HASH_TAG_DEFAULT
        set(value) = prefs.edit().putString(SEARCH_HASH_TAG, value).apply()

    var sortBy: String
        get() = prefs.getString(SORT_BY, SORT_BY_DEFAULT) ?: SORT_BY_DEFAULT
        set(value) = prefs.edit().putString(SORT_BY, value).apply()

    var language: String
        get() = prefs.getString(SELECTED_LANGUAGE, SELECTED_LANGUAGE_DEFAULT) ?: SELECTED_LANGUAGE_DEFAULT
        set(value) = setLang(value)

    var themeMode: String
        get() = ThemeMode.normalize(prefs.getString(THEME_MODE, ThemeMode.SYSTEM))
        set(value) = prefs.edit().putString(THEME_MODE, ThemeMode.normalize(value)).apply()

    var chartStyle: String
        get() = prefs.getString(CHART_STYLE, CHART_STYLE_LINE)?.takeIf { it == CHART_STYLE_CANDLES } ?: CHART_STYLE_LINE
        set(value) = prefs.edit().putString(CHART_STYLE, if (value == CHART_STYLE_CANDLES) CHART_STYLE_CANDLES else CHART_STYLE_LINE).apply()

    var activePortfolioId: Long
        get() = prefs.getLong(ACTIVE_PORTFOLIO_ID, ALL_PORTFOLIOS_ID)
        set(value) = prefs.edit().putLong(ACTIVE_PORTFOLIO_ID, value).apply()

    /** True when transactions changed since the portfolio value history was last rebuilt. */
    var historyDirty: Boolean
        get() = prefs.getBoolean(HISTORY_DIRTY, true)
        set(value) = prefs.edit().putBoolean(HISTORY_DIRTY, value).apply()

    /** Ask for a fingerprint, face or the device screen lock when the app is opened. */
    var appLockEnabled: Boolean
        get() = prefs.getBoolean(APP_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(APP_LOCK_ENABLED, value).apply()

    var currencyCode: String
        get() = prefs.getString(CURRENCY_CODE, AppCurrency.USD.code) ?: AppCurrency.USD.code
        set(value) = prefs.edit().putString(CURRENCY_CODE, AppCurrency.fromCode(value).code).apply()

    val fxRatesUpdatedAt: Long
        get() = prefs.getLong(FX_RATES_UPDATED_AT, 0L)

    fun fxRates(): Map<String, Double> =
        AppCurrency.values().mapNotNull { currency ->
            prefs.getString(FX_RATE_PREFIX + currency.code, null)?.toDoubleOrNull()?.let { currency.code to it }
        }.toMap()

    fun saveFxRates(rates: Map<String, Double>) {
        val editor = prefs.edit()
        rates.forEach { (code, rate) -> editor.putString(FX_RATE_PREFIX + code, rate.toString()) }
        editor.putLong(FX_RATES_UPDATED_AT, System.currentTimeMillis()).apply()
    }

    var newsNotes: String
        get() = prefs.getString(INSIGHTS_NEWS_NOTES, "") ?: ""
        set(value) = prefs.edit().putString(INSIGHTS_NEWS_NOTES, value).apply()

    var topCoinsLastUpdated: Long
        get() = prefs.getLong(TOP_COINS_LAST_UPDATED, 0L)
        set(value) = prefs.edit().putLong(TOP_COINS_LAST_UPDATED, value).apply()

    fun getCoinNote(symbol: String): String =
            prefs.getString(INSIGHTS_NOTE_PREFIX + symbol.uppercase(Locale.US), "") ?: ""

    fun setCoinNote(symbol: String, note: String) {
        prefs.edit().putString(INSIGHTS_NOTE_PREFIX + symbol.uppercase(Locale.US), note).apply()
    }

    fun getNewsNoteEntries(): List<String> = noteEntries(
            NOTES_NEWS_LIST,
            newsNotes
    )

    fun addNewsNote(note: String) {
        saveNote(
                listKey = NOTES_NEWS_LIST,
                legacyKey = INSIGHTS_NEWS_NOTES,
                existingNotes = getNewsNoteEntries(),
                note = note
        )
    }

    fun updateNewsNote(index: Int, note: String): Boolean = updateNote(
            listKey = NOTES_NEWS_LIST,
            legacyKey = INSIGHTS_NEWS_NOTES,
            existingNotes = getNewsNoteEntries(),
            index = index,
            note = note
    )

    fun deleteNewsNote(index: Int): Boolean = deleteNote(
            listKey = NOTES_NEWS_LIST,
            legacyKey = INSIGHTS_NEWS_NOTES,
            existingNotes = getNewsNoteEntries(),
            index = index
    )

    fun getCoinNoteEntries(symbol: String): List<String> {
        val normalizedSymbol = symbol.uppercase(Locale.US)
        return noteEntries(
                NOTES_COIN_LIST_PREFIX + normalizedSymbol,
                getCoinNote(normalizedSymbol)
        )
    }

    fun addCoinNote(symbol: String, note: String) {
        val normalizedSymbol = symbol.uppercase(Locale.US)
        saveNote(
                listKey = NOTES_COIN_LIST_PREFIX + normalizedSymbol,
                legacyKey = INSIGHTS_NOTE_PREFIX + normalizedSymbol,
                existingNotes = getCoinNoteEntries(normalizedSymbol),
                note = note
        )
    }

    fun updateCoinNote(symbol: String, index: Int, note: String): Boolean {
        val normalizedSymbol = symbol.uppercase(Locale.US)
        return updateNote(
                listKey = NOTES_COIN_LIST_PREFIX + normalizedSymbol,
                legacyKey = INSIGHTS_NOTE_PREFIX + normalizedSymbol,
                existingNotes = getCoinNoteEntries(normalizedSymbol),
                index = index,
                note = note
        )
    }

    fun deleteCoinNote(symbol: String, index: Int): Boolean {
        val normalizedSymbol = symbol.uppercase(Locale.US)
        return deleteNote(
                listKey = NOTES_COIN_LIST_PREFIX + normalizedSymbol,
                legacyKey = INSIGHTS_NOTE_PREFIX + normalizedSymbol,
                existingNotes = getCoinNoteEntries(normalizedSymbol),
                index = index
        )
    }

    fun ensureCoinTracking(coins: List<Coin>) {
        val editor = prefs.edit()
        val trackedAt = System.currentTimeMillis()
        var changed = false
        coins.forEach { coin ->
            val key = coin.from.uppercase(Locale.US)
            if (!prefs.contains(INSIGHTS_TRACKED_DATE_PREFIX + key)) {
                editor
                        .putLong(INSIGHTS_TRACKED_DATE_PREFIX + key, trackedAt)
                        .putFloat(INSIGHTS_TRACKED_PRICE_PREFIX + key, coin.priceRaw)
                changed = true
            }
        }
        if (changed) editor.apply()
    }

    fun ensureCoinTracking(symbol: String, price: Float) {
        val key = symbol.uppercase(Locale.US)
        if (!prefs.contains(INSIGHTS_TRACKED_DATE_PREFIX + key)) {
            prefs.edit()
                    .putLong(INSIGHTS_TRACKED_DATE_PREFIX + key, System.currentTimeMillis())
                    .putFloat(INSIGHTS_TRACKED_PRICE_PREFIX + key, price)
                    .apply()
        }
    }

    fun getTrackedDate(symbol: String): Long =
            prefs.getLong(INSIGHTS_TRACKED_DATE_PREFIX + symbol.uppercase(Locale.US), 0L)

    fun getTrackedPrice(symbol: String): Float =
            prefs.getFloat(INSIGHTS_TRACKED_PRICE_PREFIX + symbol.uppercase(Locale.US), 0f)

    private fun noteEntries(listKey: String, legacyNote: String): List<String> {
        val storedNotes = NoteListCodec.decode(prefs.getString(listKey, "").orEmpty())
        return if (legacyNote.isNotBlank() && storedNotes.lastOrNull() != legacyNote) {
            storedNotes + legacyNote
        } else {
            storedNotes
        }
    }

    private fun saveNote(
            listKey: String,
            legacyKey: String,
            existingNotes: List<String>,
            note: String
    ) {
        saveNotes(listKey, legacyKey, existingNotes + note)
    }

    private fun updateNote(
            listKey: String,
            legacyKey: String,
            existingNotes: List<String>,
            index: Int,
            note: String
    ): Boolean {
        if (index !in existingNotes.indices) return false
        val updatedNotes = existingNotes.toMutableList().apply { this[index] = note }
        saveNotes(listKey, legacyKey, updatedNotes)
        return true
    }

    private fun deleteNote(
            listKey: String,
            legacyKey: String,
            existingNotes: List<String>,
            index: Int
    ): Boolean {
        if (index !in existingNotes.indices) return false
        val updatedNotes = existingNotes.toMutableList().apply { removeAt(index) }
        saveNotes(listKey, legacyKey, updatedNotes)
        return true
    }

    private fun saveNotes(listKey: String, legacyKey: String, notes: List<String>) {
        prefs.edit()
                .putString(listKey, NoteListCodec.encode(notes))
                .putString(legacyKey, notes.lastOrNull().orEmpty())
                .apply()
    }

    private fun setLang(value: String) {
        prefs.edit().putString(SELECTED_LANGUAGE, value).apply()
    }

    private fun migrateLegacyNewsSearchDefault() {
        if (prefs.getBoolean(NEWS_SEARCH_DEFAULT_MIGRATED, false)) return
        val storedQuery = prefs.getString(SEARCH_HASH_TAG, null)
        prefs.edit()
                .apply {
                    if (storedQuery.equals(LEGACY_SEARCH_HASH_TAG_DEFAULT, ignoreCase = true)) {
                        remove(SEARCH_HASH_TAG)
                    }
                    putBoolean(NEWS_SEARCH_DEFAULT_MIGRATED, true)
                }
                .apply()
    }
}