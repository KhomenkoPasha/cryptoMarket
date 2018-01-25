package app.khom.pavlo.crypto.model

import android.content.Context
import app.khom.pavlo.crypto.ui.main.SortDialog

class Preferences(context: Context) {

    companion object {
        val PREFS_NAME = "com.rmnivnv.cryptomoon"
        val SEARCH_HASH_TAG = "search_hash_tag"
        val SEARCH_HASH_TAG_DEFAULT = "cryptocurrency"
        val SORT_BY = "coins_sort_by"
        val SORT_BY_DEFAULT = SortDialog.SORT_BY_NAME
        val SELECTED_LANGUAGE = "selected_language"
        val SELECTED_LANGUAGE_DEFAULT = ""
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var searchHashTag: String
        get() = prefs.getString(SEARCH_HASH_TAG, SEARCH_HASH_TAG_DEFAULT)
        set(value) = prefs.edit().putString(SEARCH_HASH_TAG, value).apply()

    var sortBy: String
        get() = prefs.getString(SORT_BY, SORT_BY_DEFAULT)
        set(value) = prefs.edit().putString(SORT_BY, value).apply()

    var language: String
        get() = prefs.getString(SELECTED_LANGUAGE, SELECTED_LANGUAGE_DEFAULT)
        set(value) = setLang(value)

    private fun setLang(value: String) {
        prefs.edit().putString(SELECTED_LANGUAGE, value).commit()
    }

}