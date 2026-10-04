package app.khom.pavlo.crypto.model

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

/** User-selected appearance. Stored in [Preferences] and mirrored here for code without a Context. */
object ThemeMode {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"

    val all = listOf(SYSTEM, LIGHT, DARK)

    @Volatile
    var current: String = SYSTEM
        private set

    fun normalize(value: String?): String = all.firstOrNull { it == value } ?: SYSTEM

    /** Stores [mode] as the active one and tells AppCompat to recreate any running screens. */
    fun apply(mode: String?) {
        current = normalize(mode)
        AppCompatDelegate.setDefaultNightMode(
            when (current) {
                LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    /** Whether resources resolved through [context] should use the dark palette under the current mode. */
    fun isNight(context: Context): Boolean = when (current) {
        LIGHT -> false
        DARK -> true
        else -> systemIsNight(context)
    }

    fun systemIsNight(context: Context): Boolean =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
}
