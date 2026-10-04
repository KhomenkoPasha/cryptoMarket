package app.khom.pavlo.crypto.model

import java.math.BigDecimal
import java.math.MathContext

/** Currencies the user can display amounts in. All stored data stays in USD. */
enum class AppCurrency(val code: String, val symbol: String) {
    USD("USD", "$"),
    EUR("EUR", "€"),
    UAH("UAH", "₴"),
    RUB("RUB", "₽"),
    BTC("BTC", "₿");

    companion object {
        fun fromCode(code: String?): AppCurrency =
            values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: USD
    }
}

/**
 * Process-wide display currency. Prices, holdings and transactions are persisted in USD; this
 * converts them at the edge when they are shown and converts user input back before it is saved.
 */
object CurrencyManager {

    private val mathContext = MathContext.DECIMAL64

    @Volatile
    var selected: AppCurrency = AppCurrency.USD
        private set

    @Volatile
    private var rates: Map<String, Double> = mapOf(AppCurrency.USD.code to 1.0)

    /** Units of the selected currency per one USD. */
    val rate: Double get() = rateOf(selected) ?: 1.0

    fun rateOf(currency: AppCurrency): Double? =
        if (currency == AppCurrency.USD) 1.0 else rates[currency.code]?.takeIf { it > 0.0 && it.isFinite() }

    fun hasRate(currency: AppCurrency): Boolean = rateOf(currency) != null

    /** Sets the active currency and rates directly; used by tests and as the single mutation point. */
    internal fun configure(currency: AppCurrency, newRates: Map<String, Double>) {
        rates = newRates + (AppCurrency.USD.code to 1.0)
        selected = if (hasRate(currency)) currency else AppCurrency.USD
    }

    fun load(preferences: Preferences) {
        rates = preferences.fxRates() + (AppCurrency.USD.code to 1.0)
        selected = AppCurrency.fromCode(preferences.currencyCode).takeIf { hasRate(it) } ?: AppCurrency.USD
    }

    fun select(currency: AppCurrency, preferences: Preferences) {
        selected = if (hasRate(currency)) currency else AppCurrency.USD
        preferences.currencyCode = selected.code
    }

    fun updateRates(newRates: Map<String, Double>, preferences: Preferences) {
        val valid = newRates.filterValues { it > 0.0 && it.isFinite() }
        if (valid.isEmpty()) return
        rates = rates + valid + (AppCurrency.USD.code to 1.0)
        preferences.saveFxRates(valid)
    }

    fun fromUsd(usd: BigDecimal): BigDecimal =
        if (selected == AppCurrency.USD) usd else usd.multiply(BigDecimal.valueOf(rate), mathContext)

    fun fromUsd(usd: Double): Double = usd * rate

    fun toUsd(amount: BigDecimal): BigDecimal =
        if (selected == AppCurrency.USD) amount else amount.divide(BigDecimal.valueOf(rate), mathContext)
}
