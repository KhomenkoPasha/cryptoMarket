package app.khom.pavlo.crypto.model

import android.content.Context
import app.khom.pavlo.crypto.model.network.NetworkRequests
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface CurrencyEntryPoint {
    fun networkRequests(): NetworkRequests
    fun preferences(): Preferences
}

/** Keeps [CurrencyManager] rates fresh. Safe to call from anywhere; a recent cache short-circuits. */
object CurrencyRateUpdater {

    private const val STALE_AFTER_MS = 60L * 60L * 1000L

    /** Loads persisted settings into [CurrencyManager] and [ThemeMode]; call once from Application.onCreate. */
    fun initialize(context: Context) {
        val preferences = entryPoint(context).preferences()
        CurrencyManager.load(preferences)
        ThemeMode.apply(preferences.themeMode)
    }

    fun refreshAsync(context: Context, force: Boolean = false, onDone: ((Boolean) -> Unit)? = null): Disposable? {
        val entryPoint = entryPoint(context)
        val preferences = entryPoint.preferences()
        if (!force && !isStale(preferences)) {
            onDone?.invoke(true)
            return null
        }
        return entryPoint.networkRequests().getFxRates()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { rates ->
                    CurrencyManager.updateRates(rates, preferences)
                    onDone?.invoke(true)
                },
                { onDone?.invoke(false) }
            )
    }

    /** Blocking variant for background workers. Returns true when rates are current afterwards. */
    fun refreshBlocking(context: Context, force: Boolean = false): Boolean {
        val entryPoint = entryPoint(context)
        val preferences = entryPoint.preferences()
        if (!force && !isStale(preferences)) return true
        return runCatching {
            CurrencyManager.updateRates(entryPoint.networkRequests().getFxRates().blockingGet(), preferences)
        }.isSuccess
    }

    private fun isStale(preferences: Preferences): Boolean =
        System.currentTimeMillis() - preferences.fxRatesUpdatedAt > STALE_AFTER_MS

    private fun entryPoint(context: Context): CurrencyEntryPoint =
        EntryPointAccessors.fromApplication(context.applicationContext, CurrencyEntryPoint::class.java)
}
