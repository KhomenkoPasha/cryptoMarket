package app.khom.pavlo.crypto.ui.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WallpaperAppearanceTest {

    private fun appearance(transparency: Int, dimming: Int) =
        WallpaperAppearance(WallpaperSettings.options.first(), transparency, dimming)

    @Test
    fun `surface opacity is what transparency leaves`() {
        assertEquals(1f, appearance(transparency = 0, dimming = 0).surfaceOpacity, 0.0001f)
        assertEquals(0.7f, appearance(transparency = 30, dimming = 0).surfaceOpacity, 0.0001f)
        assertEquals(0.3f, appearance(transparency = 70, dimming = 0).surfaceOpacity, 0.0001f)
    }

    @Test
    fun `dimming becomes an alpha channel value`() {
        assertEquals(0, appearance(transparency = 0, dimming = 0).dimmingAlpha)
        assertEquals(140, appearance(transparency = 0, dimming = 55).dimmingAlpha)
        assertEquals(204, appearance(transparency = 0, dimming = 80).dimmingAlpha)
    }

    @Test
    fun `the gallery has the seven bundled templates in order`() {
        val options = WallpaperSettings.options

        assertEquals((1..7).toList(), options.map { it.number })
        assertEquals(
            listOf(
                "wallpaper_01.webp", "wallpaper_02.webp", "wallpaper_03.webp", "wallpaper_04.webp",
                "wallpaper_05.webp", "wallpaper_06.webp", "wallpaper_07.webp"
            ),
            options.map { it.fileName }
        )
    }

    @Test
    fun `a template is loaded from the app assets`() {
        assertEquals("file:///android_asset/wallpapers/wallpaper_03.webp", WallpaperSettings.options[2].assetUri)
    }

    @Test
    fun `no appearance without a wallpaper is a valid state`() {
        assertNull(WallpaperAppearance(null, 30, 55).wallpaper)
    }

    @Test
    fun `limits keep panels and text legible`() {
        assertEquals(70, WallpaperSettings.MAX_TRANSPARENCY)
        assertEquals(80, WallpaperSettings.MAX_DIMMING)
    }
}
