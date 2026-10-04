package app.khom.pavlo.crypto.ui.wallpaper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WallpaperSettingsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clean() = reset()

    @After
    fun restore() = reset()

    private fun reset() {
        WallpaperSettings.preferences(context).edit().clear().commit()
        WallpaperSettings.invalidate()
    }

    @Test
    fun everyBundledTemplateExistsInAssets() {
        val bundled = context.assets.list("wallpapers").orEmpty().toSet()

        WallpaperSettings.options.forEach { option ->
            assertEquals("${option.fileName} is missing from assets", true, option.fileName in bundled)
        }
    }

    @Test
    fun theBundledTemplatesAreDecodable() {
        WallpaperSettings.options.forEach { option ->
            val bitmap = context.assets.open("wallpapers/${option.fileName}").use {
                android.graphics.BitmapFactory.decodeStream(it)
            }
            assertNotNull("${option.fileName} did not decode", bitmap)
        }
    }

    @Test
    fun startsWithoutAWallpaperAndWithTheDefaultAppearance() {
        val appearance = WallpaperSettings.read(context)

        assertNull(appearance.wallpaper)
        assertEquals(30, appearance.transparency)
        assertEquals(55, appearance.dimming)
    }

    @Test
    fun aChoiceIsRememberedAndCanBeCleared() {
        val third = WallpaperSettings.options[2]

        WallpaperSettings.select(context, third)
        assertEquals(third, WallpaperSettings.read(context).wallpaper)

        WallpaperSettings.select(context, null)
        assertNull(WallpaperSettings.read(context).wallpaper)
    }

    @Test
    fun sliderValuesAreStoredWithinTheirLimits() {
        WallpaperSettings.setTransparency(context, 500)
        WallpaperSettings.setDimming(context, 500)
        val high = WallpaperSettings.read(context)
        assertEquals(WallpaperSettings.MAX_TRANSPARENCY, high.transparency)
        assertEquals(WallpaperSettings.MAX_DIMMING, high.dimming)

        WallpaperSettings.setTransparency(context, -5)
        WallpaperSettings.setDimming(context, -5)
        val low = WallpaperSettings.read(context)
        assertEquals(0, low.transparency)
        assertEquals(0, low.dimming)
    }

    @Test
    fun anUnknownStoredFileCountsAsNoWallpaper() {
        WallpaperSettings.preferences(context).edit().putString("image", "wallpaper_99.webp").commit()
        WallpaperSettings.invalidate()

        assertNull(WallpaperSettings.read(context).wallpaper)
    }
}
