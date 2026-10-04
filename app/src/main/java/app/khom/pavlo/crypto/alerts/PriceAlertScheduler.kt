package app.khom.pavlo.crypto.alerts

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.network.NetworkRequests
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface PriceAlertEntryPoint {
    fun database(): CMDatabase
    fun networkRequests(): NetworkRequests
}

/** Keeps the background price check scheduled only while at least one alert is armed. */
object PriceAlertScheduler {

    private const val WORK_NAME = "price_alert_check"
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "crypto-alert-scheduler") }

    fun ensureScheduledAsync(context: Context) {
        val appContext = context.applicationContext
        executor.execute { ensureScheduled(appContext) }
    }

    fun ensureScheduled(context: Context) {
        val appContext = context.applicationContext
        val database = EntryPointAccessors.fromApplication(appContext, PriceAlertEntryPoint::class.java).database()
        if (database.priceAlertsDao().activeCountSync() > 0) {
            schedule(appContext)
        } else {
            WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)
        }
    }

    private fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<PriceAlertWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
