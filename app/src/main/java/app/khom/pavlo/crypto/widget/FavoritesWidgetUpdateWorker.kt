package app.khom.pavlo.crypto.widget

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class FavoritesWidgetUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        FavoritesWidgetUpdater.updateAllSync(applicationContext)
        return Result.success()
    }
}
