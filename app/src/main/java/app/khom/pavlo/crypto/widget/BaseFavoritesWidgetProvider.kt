package app.khom.pavlo.crypto.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

abstract class BaseFavoritesWidgetProvider(
    private val layoutResId: Int,
    private val maxItems: Int
) : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        FavoritesWidgetScheduler.ensureScheduled(context)
        FavoritesWidgetUpdater.updateWidgetsAsync(context, appWidgetIds, layoutResId, maxItems)
    }

    override fun onEnabled(context: Context) {
        FavoritesWidgetScheduler.ensureScheduled(context)
        FavoritesWidgetUpdater.updateAllAsync(context)
    }

    override fun onDisabled(context: Context) {
        FavoritesWidgetScheduler.ensureScheduled(context)
    }
}