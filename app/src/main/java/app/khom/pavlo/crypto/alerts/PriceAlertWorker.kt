package app.khom.pavlo.crypto.alerts

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import app.khom.pavlo.crypto.model.AppCurrency
import app.khom.pavlo.crypto.model.CurrencyManager
import app.khom.pavlo.crypto.model.CurrencyRateUpdater
import app.khom.pavlo.crypto.model.FSYMS
import app.khom.pavlo.crypto.model.LocaleManager
import app.khom.pavlo.crypto.model.PriceAlertEvaluator
import app.khom.pavlo.crypto.model.TSYMS
import app.khom.pavlo.crypto.model.USD
import app.khom.pavlo.crypto.utils.PortfolioValueFormatter
import dagger.hilt.android.EntryPointAccessors
import java.math.BigDecimal
import java.util.Locale

/** Checks every armed alert against fresh prices and posts a notification for those that fired. */
class PriceAlertWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, PriceAlertEntryPoint::class.java)
        val dao = entryPoint.database().priceAlertsDao()
        val alerts = dao.getActiveSync()
        if (alerts.isEmpty()) {
            PriceAlertScheduler.ensureScheduled(applicationContext)
            return Result.success()
        }

        // Without permission a fired alert could not be delivered, so leave every alert armed until it can be.
        if (!PriceAlertNotifier.canNotify(applicationContext)) return Result.success()

        val symbols = alerts.map { it.symbol.uppercase(Locale.US) }.distinct()
        val coins = runCatching {
            entryPoint.networkRequests().getPrice(
                mapOf(
                    FSYMS to ArrayList<String?>(symbols),
                    TSYMS to arrayListOf<String?>(USD)
                )
            ).blockingGet()
        }.getOrNull() ?: return Result.success()
        val coinsBySymbol = coins.associateBy { it.from.uppercase(Locale.US) }

        // Only fetch fresh exchange rates when an alert is expressed in a non-USD currency.
        if (alerts.any { it.alertType.isPriceTarget && AppCurrency.fromCode(it.currency) != AppCurrency.USD }) {
            CurrencyRateUpdater.refreshBlocking(applicationContext)
        }

        val localized = LocaleManager.setLocale(applicationContext)
        alerts.forEach { alert ->
            val coin = coinsBySymbol[alert.symbol.uppercase(Locale.US)] ?: return@forEach
            val currency = AppCurrency.fromCode(alert.currency)
            val rate = CurrencyManager.rateOf(currency)
            val fired = PriceAlertEvaluator.isTriggered(
                alert = alert,
                priceUsd = coin.priceRaw.toDouble(),
                changePct24h = coin.changePct24hRaw.toDouble(),
                rateToCurrency = rate
            )
            if (!fired) return@forEach

            val current = if (alert.alertType.isPriceTarget) {
                PortfolioValueFormatter.priceIn(BigDecimal.valueOf(coin.priceRaw.toDouble() * (rate ?: 1.0)), currency)
            } else {
                PortfolioValueFormatter.percent(BigDecimal.valueOf(coin.changePct24hRaw.toDouble()))
            }
            dao.markTriggeredSync(alert.id, System.currentTimeMillis(), current)
            PriceAlertNotifier.notify(localized, alert, current)
        }
        PriceAlertScheduler.ensureScheduled(applicationContext)
        return Result.success()
    }
}
