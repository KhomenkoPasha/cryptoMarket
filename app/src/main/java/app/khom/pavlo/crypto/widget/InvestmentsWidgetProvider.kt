package app.khom.pavlo.crypto.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class InvestmentsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        FavoritesWidgetScheduler.ensureScheduled(context)
        InvestmentsWidgetUpdater.updateWidgetsAsync(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        FavoritesWidgetScheduler.ensureScheduled(context)
        InvestmentsWidgetUpdater.updateAllAsync(context)
    }

    override fun onDisabled(context: Context) {
        FavoritesWidgetScheduler.ensureScheduled(context)
    }
}