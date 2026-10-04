package app.khom.pavlo.crypto.ui.common

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.utils.fearGreedLabel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class MarketTerminologyTest {
    @Test
    fun indexUsesStandardNamesInEnglishRussianAndUkrainian() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        val translations = mapOf(
            "en" to ("Fear & Greed Index" to "Greed"),
            "ru-RU" to ("Индекс страха и жадности" to "Жадность"),
            "uk" to ("Індекс страху та жадібності" to "Жадібність"),
        )
        for ((language, expected) in translations) {
            val configuration = Configuration(application.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language))
            }
            val context = application.createConfigurationContext(configuration)
            assertEquals(language, expected.first, context.getString(R.string.market_fear_greed))
            assertEquals(language, expected.second, context.getString(fearGreedLabel(65)))
        }
    }
}
