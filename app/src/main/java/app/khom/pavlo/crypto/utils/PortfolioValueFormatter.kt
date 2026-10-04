package app.khom.pavlo.crypto.utils

import app.khom.pavlo.crypto.model.AppCurrency
import app.khom.pavlo.crypto.model.CurrencyManager
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formats money for display. Every amount passed in is a USD value; it is converted to the
 * user's selected display currency here, so callers never deal with exchange rates.
 */
internal object PortfolioValueFormatter {

    private val moneyFormat = decimalFormat("#,##0.00")
    private val priceFormatLarge = decimalFormat("#,##0.00")
    private val priceFormatMedium = decimalFormat("#,##0.00##")
    private val priceFormatSmall = decimalFormat("#,##0.00####")
    private val priceFormatTiny = decimalFormat("#,##0.00########")
    private val btcFormat = decimalFormat("#,##0.00######")
    private val percentFormat = decimalFormat("#,##0.00")
    private val compactFormat = decimalFormat("#,##0.##")
    private val wholeFormat = decimalFormat("#,##0")

    fun money(value: BigDecimal): String = moneyValue(value, showPositiveSign = false)

    fun signedMoney(value: BigDecimal): String = moneyValue(value, showPositiveSign = true)

    fun price(value: BigDecimal): String = priceIn(CurrencyManager.fromUsd(value), CurrencyManager.selected)

    /** Formats an amount that is already expressed in [currency], without any conversion. */
    fun priceIn(amount: BigDecimal, currency: AppCurrency): String {
        val formatter = priceFormatter(amount.abs(), currency)
        val text = "${currency.symbol}${formatter.get()!!.format(amount.abs())}"
        return if (amount.signum() < 0) "-$text" else text
    }

    /** Price without a currency symbol, for layouts that show the symbol in a separate view. */
    fun priceNumber(value: BigDecimal): String {
        val converted = CurrencyManager.fromUsd(value)
        return priceFormatter(converted.abs(), CurrencyManager.selected).get()!!.format(converted)
    }

    /** Whole-unit amount without a currency symbol, e.g. market cap and volume columns. */
    fun wholeNumber(value: BigDecimal): String {
        val converted = CurrencyManager.fromUsd(value)
        val formatter = if (CurrencyManager.selected == AppCurrency.BTC) compactFormat else wholeFormat
        return formatter.get()!!.format(converted)
    }

    /** Large amounts such as market cap and volume: 1.23B, 456.7M. */
    fun compact(value: BigDecimal): String {
        val converted = CurrencyManager.fromUsd(value)
        val absolute = converted.abs()
        val (scaled, suffix) = when {
            absolute >= TRILLION -> absolute.divide(TRILLION) to "T"
            absolute >= BILLION -> absolute.divide(BILLION) to "B"
            absolute >= MILLION -> absolute.divide(MILLION) to "M"
            absolute >= THOUSAND -> absolute.divide(THOUSAND) to "K"
            else -> absolute to ""
        }
        val formatter = if (suffix.isEmpty() && CurrencyManager.selected == AppCurrency.BTC) btcFormat else compactFormat
        val sign = if (converted.signum() < 0) "-" else ""
        return "$sign${CurrencyManager.selected.symbol}${formatter.get()!!.format(scaled)}$suffix"
    }

    fun percent(value: BigDecimal): String {
        val sign = when {
            value.signum() > 0 -> "+"
            value.signum() < 0 -> "-"
            else -> ""
        }
        return "$sign${percentFormat.get()!!.format(value.abs())}%"
    }

    /** Plain editable number in the display currency, for pre-filling input fields. */
    fun inputAmount(usd: BigDecimal): String {
        val converted = CurrencyManager.fromUsd(usd)
        return converted.setScale(INPUT_SCALE, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }

    // Fewer decimals for large prices, more for small ones, so every price keeps useful precision.
    private fun priceFormatter(absolute: BigDecimal, currency: AppCurrency): ThreadLocal<DecimalFormat> = when {
        currency == AppCurrency.BTC -> btcFormat
        absolute >= PRICE_LARGE -> priceFormatLarge
        absolute >= BigDecimal.ONE -> priceFormatMedium
        absolute >= PRICE_SMALL -> priceFormatSmall
        else -> priceFormatTiny
    }

    private fun moneyValue(value: BigDecimal, showPositiveSign: Boolean): String {
        val converted = CurrencyManager.fromUsd(value)
        val sign = when {
            converted.signum() < 0 -> "-"
            showPositiveSign && converted.signum() > 0 -> "+"
            else -> ""
        }
        val formatter = if (CurrencyManager.selected == AppCurrency.BTC) btcFormat else moneyFormat
        return "$sign${CurrencyManager.selected.symbol}${formatter.get()!!.format(converted.abs())}"
    }

    private const val INPUT_SCALE = 8
    private val PRICE_LARGE = BigDecimal("1000")
    private val PRICE_SMALL = BigDecimal("0.01")
    private val THOUSAND = BigDecimal("1000")
    private val MILLION = BigDecimal("1000000")
    private val BILLION = BigDecimal("1000000000")
    private val TRILLION = BigDecimal("1000000000000")

    private fun decimalFormat(pattern: String): ThreadLocal<DecimalFormat> =
        ThreadLocal.withInitial {
            DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US)).apply {
                roundingMode = RoundingMode.HALF_UP
                isParseBigDecimal = true
            }
        }
}
