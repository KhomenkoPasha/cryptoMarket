package app.khom.pavlo.crypto.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import app.khom.pavlo.crypto.model.ThemeMode


class ResourceProvider(val context: Context) {

    private var themedContext: Context? = null
    private var themedNight: Boolean? = null

    fun getString(id: Int): String = context.getString(id)

    fun getString(id: Int, vararg formatArgs: Any): String = context.getString(id, *formatArgs)

    fun getDrawable(id: Int): Drawable? = ContextCompat.getDrawable(themed(), id)

    fun getColor(id: Int) = ContextCompat.getColor(themed(), id)

    fun getStringArray(id: Int): Array<String> = context.resources.getStringArray(id)

    /**
     * The application context follows the system dark mode, not the in-app theme choice, so colors
     * and drawables are resolved through a context whose night flag matches what activities show.
     */
    @Synchronized
    private fun themed(): Context {
        val night = ThemeMode.isNight(context)
        val cached = themedContext
        if (cached != null && themedNight == night) return cached
        val configuration = Configuration(context.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        return context.createConfigurationContext(configuration).also {
            themedContext = it
            themedNight = night
        }
    }
}
