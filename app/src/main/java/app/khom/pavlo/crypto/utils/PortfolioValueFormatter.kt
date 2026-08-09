package app.khom.pavlo.crypto.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

internal object PortfolioValueFormatter {

    private val moneyFormat = decimalFormat("#,##0.00")
    private val priceFormat = decimalFormat("#,##0.00########")
    private val percentFormat = decimalFormat("#,##0.00")

    fun money(value: BigDecimal): String = moneyValue(value, showPositiveSign = false)

    fun signedMoney(value: BigDecimal): String = moneyValue(value, showPositiveSign = true)

    fun price(value: BigDecimal): String = "\$${priceFormat.get()!!.format(value.abs())}".let {
        if (value.signum() < 0) "-$it" else it
    }

    fun percent(value: BigDecimal): String {
        val sign = when {
            value.signum() > 0 -> "+"
            value.signum() < 0 -> "-"
            else -> ""
        }
        return "$sign${percentFormat.get()!!.format(value.abs())}%"
    }

    private fun moneyValue(value: BigDecimal, showPositiveSign: Boolean): String {
        val sign = when {
            value.signum() < 0 -> "-"
            showPositiveSign && value.signum() > 0 -> "+"
            else -> ""
        }
        return "$sign\$${moneyFormat.get()!!.format(value.abs())}"
    }

    private fun decimalFormat(pattern: String): ThreadLocal<DecimalFormat> =
        ThreadLocal.withInitial {
            DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US)).apply {
                roundingMode = RoundingMode.HALF_UP
                isParseBigDecimal = true
            }
        }
}