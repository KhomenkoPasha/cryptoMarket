package app.khom.pavlo.crypto.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object FavoritesWidgetScheduler {

    private const val WORK_NAME = "favorites_widget_update"

    fun ensureScheduled(context: Context) {
        if (hasAnyWidgets(context)) {
            schedule(context)
        } else {
            cancel(context)
        }
    }

    private fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<FavoritesWidgetUpdateWorker>(
            15,
            TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun hasAnyWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val smallIds = manager.getAppWidgetIds(
            ComponentName(context, FavoritesWidgetSmallProvider::class.java)
        )
        val largeIds = manager.getAppWidgetIds(
            ComponentName(context, FavoritesWidgetLargeProvider::class.java)
        )
        val investmentsIds = manager.getAppWidgetIds(
            ComponentName(context, InvestmentsWidgetProvider::class.java)
        )
        return smallIds.isNotEmpty() || largeIds.isNotEmpty() || investmentsIds.isNotEmpty()
    }
}