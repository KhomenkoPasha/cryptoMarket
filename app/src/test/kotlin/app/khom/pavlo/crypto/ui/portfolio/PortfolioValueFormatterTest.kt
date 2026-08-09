package app.khom.pavlo.crypto.ui.portfolio

import app.khom.pavlo.crypto.utils.PortfolioValueFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class PortfolioValueFormatterTest {

    @Test
    fun `money uses grouped two-decimal financial format`() {
        assertEquals("\$12,230.00", PortfolioValueFormatter.money("12230".toBigDecimal()))
        assertEquals("-\$820.00", PortfolioValueFormatter.signedMoney("-820".toBigDecimal()))
    }

    @Test
    fun `percent preserves sign and rounds to two decimals`() {
        assertEquals("+16.48%", PortfolioValueFormatter.percent("16.476".toBigDecimal()))
        assertEquals("-7.81%", PortfolioValueFormatter.percent("-7.806".toBigDecimal()))
    }

    @Test
    fun `unit price retains precision needed by low-price coins`() {
        assertEquals("\$0.00001234", PortfolioValueFormatter.price("0.00001234".toBigDecimal()))
    }

    @Test
    fun `formatting stays stable across locale changes and worker threads`() {
        val originalLocale = Locale.getDefault()
        val executor = Executors.newFixedThreadPool(4)
        try {
            Locale.setDefault(Locale.GERMANY)
            val values = listOf("1", "12.3", "1234.56", "0.00001234")
            val expected = listOf("\$1.00", "\$12.30", "\$1,234.56", "\$0.00")
            val tasks = (0 until 200).map { index ->
                Callable {
                    val valueIndex = index % values.size
                    valueIndex to PortfolioValueFormatter.money(values[valueIndex].toBigDecimal())
                }
            }

            executor.invokeAll(tasks).forEach { future ->
                val (index, formatted) = future.get()
                assertEquals(expected[index], formatted)
            }
        } finally {
            executor.shutdownNow()
            Locale.setDefault(originalLocale)
        }
    }
}