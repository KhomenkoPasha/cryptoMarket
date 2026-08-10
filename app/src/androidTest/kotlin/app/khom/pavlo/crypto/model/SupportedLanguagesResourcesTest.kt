package app.khom.pavlo.crypto.model

import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.khom.pavlo.crypto.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class SupportedLanguagesResourcesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun allAdditionalLocalesResolveTranslatedCoreResources() {
        val expectedLanguageTitles = mapOf(
            SupportedLanguages.SPANISH to "Idioma",
            SupportedLanguages.FRENCH to "Langue",
            SupportedLanguages.GERMAN to "Sprache",
            SupportedLanguages.PORTUGUESE_BRAZIL to "Idioma",
            SupportedLanguages.CHINESE_SIMPLIFIED to "语言",
            SupportedLanguages.JAPANESE to "言語",
            SupportedLanguages.KOREAN to "언어",
            SupportedLanguages.HINDI to "भाषा",
            SupportedLanguages.ARABIC to "اللغة",
            SupportedLanguages.INDONESIAN to "Bahasa"
        )

        expectedLanguageTitles.forEach { (tag, expectedTitle) ->
            assertEquals(expectedTitle, localizedContext(tag).getString(R.string.language))
        }
    }

    @Test
    fun arabicResourcesUseRightToLeftLayoutDirection() {
        assertEquals(
            View.LAYOUT_DIRECTION_RTL,
            localizedContext(SupportedLanguages.ARABIC).resources.configuration.layoutDirection
        )
    }

    @Test
    fun ukrainianSettingsResourcesAreLocalized() {
        val ukrainianContext = localizedContext(SupportedLanguages.UKRAINIAN)

        assertEquals("Налаштування", ukrainianContext.getString(R.string.settings))
        assertEquals("Мова", ukrainianContext.getString(R.string.language))
        assertEquals("Вартість активів", ukrainianContext.getString(R.string.total_holdings))
        assertEquals("Розподіл", ukrainianContext.getString(R.string.allocations))
        assertEquals("прибуток", ukrainianContext.getString(R.string.profit))
        assertEquals("збиток", ukrainianContext.getString(R.string.loss))
    }

    private fun localizedContext(tag: String): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(tag))
        return context.createConfigurationContext(configuration)
    }
}