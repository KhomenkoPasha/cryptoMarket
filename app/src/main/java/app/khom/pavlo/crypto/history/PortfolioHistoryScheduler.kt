package app.khom.pavlo.crypto.history

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.db.PortfolioHistoryRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface PortfolioHistoryEntryPoint {
    fun database(): CMDatabase
    fun historyRepository(): PortfolioHistoryRepository
}

/** Takes the daily portfolio value snapshot, and catches up shortly after transactions change. */
object PortfolioHistoryScheduler {

    private const val DAILY_WORK = "portfolio_history_daily"
    private const val REFRESH_WORK = "portfolio_history_refresh"
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "crypto-history-scheduler") }
    private val networkConstraint = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun ensureScheduledAsync(context: Context) {
        val appContext = context.applicationContext
        executor.execute { ensureScheduled(appContext) }
    }

    fun ensureScheduled(context: Context) {
        val appContext = context.applicationContext
        val database = EntryPointAccessors.fromApplication(appContext, PortfolioHistoryEntryPoint::class.java).database()
        val manager = WorkManager.getInstance(appContext)
        if (database.holdingsDao().getAllHoldingsSync().isEmpty()) {
            manager.cancelUniqueWork(DAILY_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<PortfolioSnapshotWorker>(24, TimeUnit.HOURS)
            .setConstraints(networkConstraint)
            .build()
        manager.enqueueUniquePeriodicWork(DAILY_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Called after transactions change; rapid edits collapse into one refresh. */
    fun refreshSoon(context: Context) {
        val appContext = context.applicationContext
        val request = OneTimeWorkRequestBuilder<PortfolioSnapshotWorker>()
            .setConstraints(networkConstraint)
            .setInitialDelay(5, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(REFRESH_WORK, ExistingWorkPolicy.REPLACE, request)
        ensureScheduledAsync(appContext)
    }
}

class PortfolioSnapshotWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, PortfolioHistoryEntryPoint::class.java)
        val complete = runCatching { entryPoint.historyRepository().refreshBlocking() }.getOrDefault(false)
        // An incomplete run (offline) is retried by WorkManager with backoff; the daily schedule continues regardless.
        return if (complete) Result.success() else Result.retry()
    }
}
