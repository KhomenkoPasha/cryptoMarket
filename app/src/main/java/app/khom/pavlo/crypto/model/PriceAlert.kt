package app.khom.pavlo.crypto.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

enum class PriceAlertType {
    /** Price reaches or passes a target going up. */
    PRICE_ABOVE,

    /** Price reaches or passes a target going down. */
    PRICE_BELOW,

    /** 24h change is at least +threshold percent. */
    CHANGE_UP,

    /** 24h change is at most -threshold percent. */
    CHANGE_DOWN;

    val isPriceTarget: Boolean get() = this == PRICE_ABOVE || this == PRICE_BELOW

    companion object {
        fun fromName(name: String?): PriceAlertType =
            values().firstOrNull { it.name == name } ?: PRICE_ABOVE
    }
}

/**
 * A one-shot notification rule. [threshold] is a price in [currency] for price alerts and a
 * positive percentage for 24h change alerts. Once fired the alert is disabled and keeps
 * [triggeredAt] so the list can show when it happened.
 */
@Entity(tableName = "price_alerts")
data class PriceAlert(
        @PrimaryKey(autoGenerate = true) var id: Long = 0,
        var symbol: String,
        @ColumnInfo(name = "coin_name") var coinName: String = "",
        var type: String = PriceAlertType.PRICE_ABOVE.name,
        var threshold: BigDecimal = BigDecimal.ZERO,
        var currency: String = AppCurrency.USD.code,
        var enabled: Boolean = true,
        @ColumnInfo(name = "created_at") var createdAt: Long = 0L,
        @ColumnInfo(name = "triggered_at") var triggeredAt: Long = 0L,
        @ColumnInfo(name = "triggered_value") var triggeredValue: String = "") {

    val alertType: PriceAlertType get() = PriceAlertType.fromName(type)
}

/** Decides whether an alert should fire for the latest market data. Pure so it can be unit tested. */
object PriceAlertEvaluator {

    /**
     * @param priceUsd latest price in USD
     * @param changePct24h latest 24h change in percent
     * @param rateToCurrency units of the alert's currency per one USD, or null when unknown
     */
    fun isTriggered(
            alert: PriceAlert,
            priceUsd: Double,
            changePct24h: Double,
            rateToCurrency: Double?
    ): Boolean {
        val threshold = alert.threshold.toDouble()
        if (threshold <= 0.0 || !threshold.isFinite()) return false
        return when (alert.alertType) {
            PriceAlertType.PRICE_ABOVE -> priceIn(priceUsd, rateToCurrency)?.let { it >= threshold } ?: false
            PriceAlertType.PRICE_BELOW -> priceIn(priceUsd, rateToCurrency)?.let { it <= threshold } ?: false
            PriceAlertType.CHANGE_UP -> changePct24h.isFinite() && changePct24h >= threshold
            PriceAlertType.CHANGE_DOWN -> changePct24h.isFinite() && changePct24h <= -threshold
        }
    }

    private fun priceIn(priceUsd: Double, rate: Double?): Double? {
        if (!priceUsd.isFinite() || priceUsd <= 0.0 || rate == null || !rate.isFinite() || rate <= 0.0) return null
        return priceUsd * rate
    }
}
