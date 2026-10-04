package app.khom.pavlo.crypto.ui.wallpaper

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

internal data class WallpaperOption(
    val fileName: String,
    val number: Int,
) {
    val assetUri: String get() = "file:///android_asset/wallpapers/$fileName"
}

internal data class WallpaperAppearance(
    val wallpaper: WallpaperOption?,
    val transparency: Int,
    val dimming: Int,
) {
    val surfaceOpacity: Float get() = (PERCENT - transparency) / PERCENT.toFloat()

    /** Dimming as an alpha channel value, for the veil drawn over the wallpaper. */
    val dimmingAlpha: Int get() = dimming * OPAQUE_ALPHA / PERCENT

    private companion object {
        const val PERCENT = 100
        const val OPAQUE_ALPHA = 255
    }
}

internal object WallpaperSettings {
    private const val PREFS_NAME = "app_wallpaper"
    private const val KEY_IMAGE = "image"
    private const val KEY_TRANSPARENCY = "transparency"
    private const val KEY_DIMMING = "dimming"

    // Upper bounds keep panels and text legible over any wallpaper.
    private const val DEFAULT_TRANSPARENCY = 30
    const val MAX_TRANSPARENCY = 70
    private const val DEFAULT_DIMMING = 55
    const val MAX_DIMMING = 80

    val options = (1..7).map { WallpaperOption("wallpaper_${it.toString().padStart(2, '0')}.webp", it) }

    // read() sits on hot paths — every layout pass and every surface traversal — so the
    // parsed appearance is held in memory and dropped whenever a preference changes.
    @Volatile private var cached: WallpaperAppearance? = null

    private var invalidationListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun preferences(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun invalidate() {
        cached = null
    }

    fun read(context: Context): WallpaperAppearance {
        cached?.let { return it }
        val preferences = preferences(context)
        registerInvalidation(preferences)
        // No stored choice means no wallpaper: the screens look as they did until one is picked.
        val fileName = preferences.getString(KEY_IMAGE, "")
        val transparency = preferences.getInt(KEY_TRANSPARENCY, DEFAULT_TRANSPARENCY)
        val dimming = preferences.getInt(KEY_DIMMING, DEFAULT_DIMMING)
        val appearance =
            WallpaperAppearance(
                wallpaper = options.firstOrNull { it.fileName == fileName },
                transparency = transparency.coerceIn(0, MAX_TRANSPARENCY),
                dimming = dimming.coerceIn(0, MAX_DIMMING),
            )
        cached = appearance
        return appearance
    }

    /**
     * Covers writers that bypass the setters below.
     * SharedPreferences keeps listeners weakly, hence the strong field.
     */
    @Synchronized
    private fun registerInvalidation(preferences: SharedPreferences) {
        if (invalidationListener != null) return
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> invalidate() }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        invalidationListener = listener
    }

    fun select(
        context: Context,
        option: WallpaperOption?,
    ) {
        invalidate()
        preferences(context).edit { putString(KEY_IMAGE, option?.fileName.orEmpty()) }
    }

    fun setTransparency(
        context: Context,
        value: Int,
    ) {
        invalidate()
        preferences(context).edit { putInt(KEY_TRANSPARENCY, value.coerceIn(0, MAX_TRANSPARENCY)) }
    }

    fun setDimming(
        context: Context,
        value: Int,
    ) {
        invalidate()
        preferences(context).edit { putInt(KEY_DIMMING, value.coerceIn(0, MAX_DIMMING)) }
    }
}
