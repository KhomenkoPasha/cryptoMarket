package app.khom.pavlo.crypto.widget

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.SupportedLanguages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class WidgetTranslationsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun everySupportedLanguageHasLocalizedWidgetTitlesAndLabels() {
        val titles = mapOf(
            SupportedLanguages.ENGLISH to ("Favorites" to "My investments"),
            SupportedLanguages.RUSSIAN to ("Избранные" to "Мои инвестиции"),
            SupportedLanguages.UKRAINIAN to ("Вибране" to "Мої інвестиції"),
            SupportedLanguages.SPANISH to ("Favoritos" to "Mis inversiones"),
            SupportedLanguages.FRENCH to ("Favoris" to "Mes investissements"),
            SupportedLanguages.GERMAN to ("Favoriten" to "Meine Investments"),
            SupportedLanguages.PORTUGUESE_BRAZIL to ("Favoritos" to "Meus investimentos"),
            SupportedLanguages.CHINESE_SIMPLIFIED to ("收藏" to "我的投资"),
            SupportedLanguages.JAPANESE to ("お気に入り" to "マイ投資"),
            SupportedLanguages.KOREAN to ("즐겨찾기" to "내 투자"),
            SupportedLanguages.HINDI to ("पसंदीदा" to "मेरे निवेश"),
            SupportedLanguages.ARABIC to ("المفضلة" to "استثماراتي"),
            SupportedLanguages.INDONESIAN to ("Favorit" to "Investasi saya")
        )

        titles.forEach { (language, expectedTitles) ->
            val localizedContext = localizedContext(language)
            assertEquals(
                expectedTitles.first,
                localizedContext.getString(R.string.widget_favorites_title_small)
            )
            assertEquals(
                expectedTitles.second,
                localizedContext.getString(R.string.widget_investments_title)
            )
            widgetStringIds.forEach { stringId ->
                assertTrue(localizedContext.getString(stringId).isNotBlank())
            }
        }
    }

    private fun localizedContext(language: String): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return context.createConfigurationContext(configuration)
    }

    private companion object {
        val widgetStringIds = intArrayOf(
            R.string.widget_favorites_title_small,
            R.string.widget_favorites_title_large,
            R.string.widget_empty_favorites,
            R.string.widget_investments_title,
            R.string.widget_investments_description,
            R.string.widget_investments_current_value,
            R.string.widget_investments_invested,
            R.string.widget_investments_total_return,
            R.string.widget_investments_positions,
            R.string.widget_investments_empty,
            R.string.widget_investments_empty_hint,
            R.string.widget_investments_price_unavailable
        )
    }
}