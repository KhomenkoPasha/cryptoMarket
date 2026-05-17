package app.khom.pavlo.crypto.model

import android.content.Context
import app.khom.pavlo.crypto.ui.main.SortDialog
import java.util.Locale

class Preferences(context: Context) {

    companion object {
        val PREFS_NAME = "com.rmnivnv.cryptomoon"
        val SEARCH_HASH_TAG = "search_hash_tag"
        val SEARCH_HASH_TAG_DEFAULT = "cryptocurrency"
        val SORT_BY = "coins_sort_by"
        val SORT_BY_DEFAULT = SortDialog.SORT_BY_NAME
        val SELECTED_LANGUAGE = "selected_language"
        val SELECTED_LANGUAGE_DEFAULT = ""
        private const val INSIGHTS_NOTE_PREFIX = "insights_note_"
        private const val INSIGHTS_TRACKED_DATE_PREFIX = "insights_tracked_date_"
        private const val INSIGHTS_TRACKED_PRICE_PREFIX = "insights_tracked_price_"
        private const val INSIGHTS_NEWS_NOTES = "insights_news_notes"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var searchHashTag: String
        get() = prefs.getString(SEARCH_HASH_TAG, SEARCH_HASH_TAG_DEFAULT) ?: SEARCH_HASH_TAG_DEFAULT
        set(value) = prefs.edit().putString(SEARCH_HASH_TAG, value).apply()

    var sortBy: String
        get() = prefs.getString(SORT_BY, SORT_BY_DEFAULT) ?: SORT_BY_DEFAULT
        set(value) = prefs.edit().putString(SORT_BY, value).apply()

    var language: String
        get() = prefs.getString(SELECTED_LANGUAGE, SELECTED_LANGUAGE_DEFAULT) ?: SELECTED_LANGUAGE_DEFAULT
        set(value) = setLang(value)

    var newsNotes: String
        get() = prefs.getString(INSIGHTS_NEWS_NOTES, "") ?: ""
        set(value) = prefs.edit().putString(INSIGHTS_NEWS_NOTES, value).apply()

    fun getCoinNote(symbol: String): String =
            prefs.getString(INSIGHTS_NOTE_PREFIX + symbol.uppercase(Locale.US), "") ?: ""

    fun setCoinNote(symbol: String, note: String) {
        prefs.edit().putString(INSIGHTS_NOTE_PREFIX + symbol.uppercase(Locale.US), note).apply()
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

    private fun setLang(value: String) {
        prefs.edit().putString(SELECTED_LANGUAGE, value).commit()
    }

}
