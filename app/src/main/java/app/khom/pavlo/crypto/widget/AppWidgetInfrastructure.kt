package app.khom.pavlo.crypto.widget

import app.khom.pavlo.crypto.model.db.CMDatabase
import app.khom.pavlo.crypto.model.network.NetworkRequests
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface AppWidgetEntryPoint {
    fun database(): CMDatabase
    fun networkRequests(): NetworkRequests
}

internal object AppWidgetExecutor {
    val instance: ExecutorService = Executors.newSingleThreadExecutor { task ->
        Thread(task, "crypto-widget-updater")
    }
}