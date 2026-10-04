package app.khom.pavlo.crypto.widget

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import app.khom.pavlo.crypto.model.AppCurrency
import app.khom.pavlo.crypto.model.CurrencyManager
import app.khom.pavlo.crypto.model.CurrencyRateUpdater

class FavoritesWidgetUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        if (CurrencyManager.selected != AppCurrency.USD) {
            CurrencyRateUpdater.refreshBlocking(applicationContext)
        }
        FavoritesWidgetUpdater.updateAllSync(applicationContext)
        InvestmentsWidgetUpdater.updateAllSync(applicationContext)
        return Result.success()
    }
}
