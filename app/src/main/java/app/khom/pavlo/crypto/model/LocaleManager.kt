package app.khom.pavlo.crypto.model

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import java.util.Locale



class LocaleManager {

    companion object {

        val ENGLISH = "en"
        val RUSSIAN = "ru"
        val UKR = "uk"

        fun setLocale(context: Context): Context = updateResources(context, getLanguage(context))

        fun setNewLocale(context: Context, language: String): Context {
            persistLanguage(context, language)
            return updateResources(context, language)
        }

        private fun getLanguage(context: Context): String {
            val prefs = Preferences(context)
            return prefs.language
        }

        private fun persistLanguage(context: Context, language: String) {
            val prefs = Preferences(context)
            prefs.language = language
        }

        private fun updateResources(context: Context, language: String): Context {
            var contextChange = context
            val locale = if (language.isBlank()) Locale.ROOT else Locale.forLanguageTag(language)
            Locale.setDefault(locale)

            val res = contextChange.resources
            val config = Configuration(res.configuration)
            config.setLocale(locale)
            contextChange = contextChange.createConfigurationContext(config)
            return contextChange
        }

        fun getLocale(res: Resources): Locale {
            return ConfigurationCompat.getLocales(res.configuration)[0] ?: Locale.getDefault()
        }
    }

}
