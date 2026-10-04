package app.khom.pavlo.crypto.utils

import app.khom.pavlo.crypto.model.AppCurrency
import app.khom.pavlo.crypto.model.CurrencyManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class CurrencyFormattingTest {

    @After
    fun resetCurrency() {
        CurrencyManager.configure(AppCurrency.USD, emptyMap())
    }

    @Test
    fun `usd is the default and needs no rate`() {
        assertEquals("\$100.00", PortfolioValueFormatter.money(BigDecimal("100")))
        assertEquals(1.0, CurrencyManager.rate, 0.0)
    }

    @Test
    fun `amounts are converted and prefixed with the selected symbol`() {
        CurrencyManager.configure(AppCurrency.EUR, mapOf("EUR" to 0.5))
        assertEquals("€50.00", PortfolioValueFormatter.money(BigDecimal("100")))
        assertEquals("+€5.00", PortfolioValueFormatter.signedMoney(BigDecimal("10")))
        assertEquals("-€5.00", PortfolioValueFormatter.signedMoney(BigDecimal("-10")))
        assertEquals("€0.50", PortfolioValueFormatter.price(BigDecimal("1")))
    }

    @Test
    fun `selecting a currency without a rate falls back to usd`() {
        CurrencyManager.configure(AppCurrency.UAH, mapOf("EUR" to 0.5))
        assertEquals(AppCurrency.USD, CurrencyManager.selected)
        assertEquals("\$10.00", PortfolioValueFormatter.money(BigDecimal("10")))
    }

    @Test
    fun `user input converts back to usd`() {
        CurrencyManager.configure(AppCurrency.UAH, mapOf("UAH" to 40.0))
        assertEquals(0, BigDecimal("2.5").compareTo(CurrencyManager.toUsd(BigDecimal("100"))))
        assertEquals(0, BigDecimal("400").compareTo(CurrencyManager.fromUsd(BigDecimal("10"))))
    }

    @Test
    fun `bitcoin keeps satoshi level precision`() {
        CurrencyManager.configure(AppCurrency.BTC, mapOf("BTC" to 0.00001))
        assertEquals("₿1.00", PortfolioValueFormatter.money(BigDecimal("100000")))
        assertEquals("₿0.00123456", PortfolioValueFormatter.price(BigDecimal("123.456")))
    }

    @Test
    fun `price keeps fewer decimals for large prices and more for small ones`() {
        assertEquals("\$85,332.83", PortfolioValueFormatter.price(BigDecimal("85332.8338281527")))
        assertEquals("\$12.3457", PortfolioValueFormatter.price(BigDecimal("12.3456789")))
        assertEquals("\$0.50", PortfolioValueFormatter.price(BigDecimal("0.5")))
        assertEquals("\$0.123457", PortfolioValueFormatter.price(BigDecimal("0.1234567")))
        assertEquals("\$0.00001234", PortfolioValueFormatter.price(BigDecimal("0.00001234")))
    }

    @Test
    fun `price in formats an amount without converting it`() {
        CurrencyManager.configure(AppCurrency.EUR, mapOf("EUR" to 0.5))
        assertEquals("₴1,500.00", PortfolioValueFormatter.priceIn(BigDecimal("1500"), AppCurrency.UAH))
        assertEquals("-\$2.00", PortfolioValueFormatter.priceIn(BigDecimal("-2"), AppCurrency.USD))
    }

    @Test
    fun `compact shortens large amounts`() {
        assertEquals("\$3.04T", PortfolioValueFormatter.compact(BigDecimal("3040000000000")))
        assertEquals("\$1.72B", PortfolioValueFormatter.compact(BigDecimal("1720000000")))
        assertEquals("\$456.7M", PortfolioValueFormatter.compact(BigDecimal("456700000")))
        assertEquals("\$12.5K", PortfolioValueFormatter.compact(BigDecimal("12500")))
        assertEquals("\$999", PortfolioValueFormatter.compact(BigDecimal("999")))
    }

    @Test
    fun `input amount is plain and trimmed`() {
        CurrencyManager.configure(AppCurrency.EUR, mapOf("EUR" to 0.5))
        assertEquals("50", PortfolioValueFormatter.inputAmount(BigDecimal("100")))
        assertEquals("0.00000125", PortfolioValueFormatter.inputAmount(BigDecimal("0.0000025")))
    }
}
