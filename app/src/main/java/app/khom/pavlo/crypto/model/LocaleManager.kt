package app.khom.pavlo.crypto.model

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.core.os.ConfigurationCompat
import java.util.Locale



class LocaleManager {

    companion object {

        fun setLocale(context: Context): Context = updateResources(context, getLanguage(context))

        fun setNewLocale(context: Context, language: String): Context {
            val supportedLanguage = SupportedLanguages.normalize(language) ?: SupportedLanguages.ENGLISH
            persistLanguage(context, supportedLanguage)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.getSystemService(android.app.LocaleManager::class.java)
                    .applicationLocales = LocaleList.forLanguageTags(supportedLanguage)
            }
            return updateResources(context, supportedLanguage)
        }

        private fun getLanguage(context: Context): String {
            val prefs = Preferences(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val systemLanguage = context.getSystemService(android.app.LocaleManager::class.java)
                    .applicationLocales
                    .toLanguageTags()
                    .substringBefore(',')
                    .let(SupportedLanguages::normalize)
                if (systemLanguage != null) {
                    prefs.language = systemLanguage
                    return systemLanguage
                }
            }
            return prefs.language
        }

        private fun persistLanguage(context: Context, language: String) {
            val prefs = Preferences(context)
            prefs.language = language
        }

        @SuppressLint("AppBundleLocaleChanges")
        private fun updateResources(context: Context, language: String): Context {
            if (language.isBlank()) return context
            var contextChange = context
            val locale = Locale.forLanguageTag(
                SupportedLanguages.normalize(language) ?: SupportedLanguages.ENGLISH
            )
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