package app.khom.pavlo.crypto.model

import java.util.Locale

data class AppLanguage(val tag: String)

object SupportedLanguages {
    const val ENGLISH = "en"
    const val RUSSIAN = "ru"
    const val UKRAINIAN = "uk"
    const val SPANISH = "es"
    const val FRENCH = "fr"
    const val GERMAN = "de"
    const val PORTUGUESE_BRAZIL = "pt-BR"
    const val CHINESE_SIMPLIFIED = "zh-CN"
    const val JAPANESE = "ja"
    const val KOREAN = "ko"
    const val HINDI = "hi"
    const val ARABIC = "ar"
    const val INDONESIAN = "id"

    val all: List<AppLanguage> = listOf(
        AppLanguage(ENGLISH),
        AppLanguage(RUSSIAN),
        AppLanguage(UKRAINIAN),
        AppLanguage(SPANISH),
        AppLanguage(FRENCH),
        AppLanguage(GERMAN),
        AppLanguage(PORTUGUESE_BRAZIL),
        AppLanguage(CHINESE_SIMPLIFIED),
        AppLanguage(JAPANESE),
        AppLanguage(KOREAN),
        AppLanguage(HINDI),
        AppLanguage(ARABIC),
        AppLanguage(INDONESIAN)
    )

    fun normalize(tag: String?): String? {
        if (tag.isNullOrBlank()) return null
        all.firstOrNull { it.tag.equals(tag, ignoreCase = true) }?.let { return it.tag }
        val language = Locale.forLanguageTag(tag).language
        return all.firstOrNull { Locale.forLanguageTag(it.tag).language == language }?.tag
    }

    fun nativeDisplayName(tag: String): String {
        val locale = Locale.forLanguageTag(normalize(tag) ?: ENGLISH)
        return locale.getDisplayName(locale).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }
    }
}