package app.khom.pavlo.crypto.ui.coins

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.widget.ImageButton
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.widget.ImageViewCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.ui.addCoin.AddCoinActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class FavoriteRemoveButtonTest {
    @Test
    fun lightThemeUsesSmallLocalizedUnfavoriteAction() = checkAction(Configuration.UI_MODE_NIGHT_NO)

    @Test
    fun darkThemeUsesSmallLocalizedUnfavoriteAction() = checkAction(Configuration.UI_MODE_NIGHT_YES)

    private fun checkAction(nightMode: Int) {
        ActivityScenario.launch(AddCoinActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val labels = mapOf(
                    "en" to "Remove from favorites",
                    "ru-RU" to "Удалить из избранного",
                    "uk" to "Видалити з обраного",
                )
                for ((language, label) in labels) {
                    val configuration = Configuration(activity.resources.configuration).apply {
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
                        setLocale(Locale.forLanguageTag(language))
                    }
                    val context = ContextThemeWrapper(activity.createConfigurationContext(configuration), R.style.AppTheme)
                    val row = LayoutInflater.from(activity).cloneInContext(context).inflate(R.layout.coins_list_item, null)
                    val button = row.findViewById<ImageButton>(R.id.main_item_remove_favorite)
                    assertEquals(label, button.contentDescription.toString())
                    assertEquals(label, button.tooltipText.toString())
                    assertTrue(button.isClickable)
                    val tint = context.getColor(R.color.on_surface_variant)
                    assertEquals(tint, requireNotNull(ImageViewCompat.getImageTintList(button)).defaultColor)
                    val expected = requireNotNull(AppCompatResources.getDrawable(context, R.drawable.ic_remove_favorite))
                        .mutate().apply { setTint(tint) }
                    val size = (16 * context.resources.displayMetrics.density).roundToInt()
                    val actualBitmap = render(requireNotNull(button.drawable), size)
                    val expectedBitmap = render(expected, size)
                    try {
                        assertTrue("Favorites action must use the crossed-out heart", actualBitmap.sameAs(expectedBitmap))
                        assertEquals(size, button.layoutParams.width - button.paddingLeft - button.paddingRight)
                        val touchSize = (36 * context.resources.displayMetrics.density).roundToInt()
                        assertEquals(touchSize, button.layoutParams.width)
                        assertEquals(touchSize, button.layoutParams.height)
                    } finally {
                        actualBitmap.recycle()
                        expectedBitmap.recycle()
                    }
                }
            }
        }
    }

    private fun render(drawable: Drawable, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }
}
