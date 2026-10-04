package app.khom.pavlo.crypto.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.AppCurrency
import app.khom.pavlo.crypto.model.NAME
import app.khom.pavlo.crypto.model.PriceAlert
import app.khom.pavlo.crypto.model.PriceAlertType
import app.khom.pavlo.crypto.model.TO
import app.khom.pavlo.crypto.model.USD
import app.khom.pavlo.crypto.ui.coinInfo.CoinInfoActivity
import app.khom.pavlo.crypto.utils.PortfolioValueFormatter

object PriceAlertNotifier {

    private const val CHANNEL_ID = "price_alerts"

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** True when a notification posted now would actually be shown (permission granted, not switched off). */
    fun canNotify(context: Context): Boolean =
        hasPermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = context.getString(R.string.alert_channel_description) }
        manager.createNotificationChannel(channel)
    }

    /** Text describing the rule, for example "BTC rises above $90,000.00". */
    fun summary(context: Context, alert: PriceAlert): String {
        val value = formatThreshold(alert)
        return when (alert.alertType) {
            PriceAlertType.PRICE_ABOVE -> context.getString(R.string.alert_summary_above, alert.symbol, value)
            PriceAlertType.PRICE_BELOW -> context.getString(R.string.alert_summary_below, alert.symbol, value)
            PriceAlertType.CHANGE_UP -> context.getString(R.string.alert_summary_change_up, alert.symbol, value)
            PriceAlertType.CHANGE_DOWN -> context.getString(R.string.alert_summary_change_down, alert.symbol, value)
        }
    }

    fun formatThreshold(alert: PriceAlert): String =
        if (alert.alertType.isPriceTarget) {
            PortfolioValueFormatter.priceIn(alert.threshold, AppCurrency.fromCode(alert.currency))
        } else {
            PortfolioValueFormatter.percent(alert.threshold).removePrefix("+")
        }

    /** @param current the value that fired the alert, already formatted for display. */
    fun notify(context: Context, alert: PriceAlert, current: String) {
        if (!canNotify(context)) return
        ensureChannel(context)

        val openCoin = Intent(context, CoinInfoActivity::class.java)
            .putExtra(NAME, alert.symbol)
            .putExtra(TO, USD)
        val pendingIntent = TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(openCoin)
            .getPendingIntent(
                alert.id.toInt(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val text = when (alert.alertType) {
            PriceAlertType.PRICE_ABOVE ->
                context.getString(R.string.alert_notification_above, alert.symbol, current, formatThreshold(alert))
            PriceAlertType.PRICE_BELOW ->
                context.getString(R.string.alert_notification_below, alert.symbol, current, formatThreshold(alert))
            PriceAlertType.CHANGE_UP ->
                context.getString(R.string.alert_notification_change_up, alert.symbol, current)
            PriceAlertType.CHANGE_DOWN ->
                context.getString(R.string.alert_notification_change_down, alert.symbol, current)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setContentTitle(context.getString(R.string.alert_notification_title, alert.symbol))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(alert.id.toInt(), notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the call; the alert is still marked fired.
        }
    }
}
