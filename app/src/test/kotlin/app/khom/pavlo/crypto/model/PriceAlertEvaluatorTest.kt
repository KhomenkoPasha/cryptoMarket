package app.khom.pavlo.crypto.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceAlertEvaluatorTest {

    private fun alert(type: PriceAlertType, threshold: String, currency: String = "USD") = PriceAlert(
        symbol = "BTC",
        type = type.name,
        threshold = threshold.toBigDecimal(),
        currency = currency
    )

    @Test
    fun `price above fires at and over the target`() {
        val alert = alert(PriceAlertType.PRICE_ABOVE, "90000")
        assertFalse(PriceAlertEvaluator.isTriggered(alert, 89_999.99, 0.0, 1.0))
        assertTrue(PriceAlertEvaluator.isTriggered(alert, 90_000.0, 0.0, 1.0))
        assertTrue(PriceAlertEvaluator.isTriggered(alert, 95_000.0, 0.0, 1.0))
    }

    @Test
    fun `price below fires at and under the target`() {
        val alert = alert(PriceAlertType.PRICE_BELOW, "80000")
        assertFalse(PriceAlertEvaluator.isTriggered(alert, 80_000.01, 0.0, 1.0))
        assertTrue(PriceAlertEvaluator.isTriggered(alert, 80_000.0, 0.0, 1.0))
        assertTrue(PriceAlertEvaluator.isTriggered(alert, 70_000.0, 0.0, 1.0))
    }

    @Test
    fun `price targets are compared in the alert currency`() {
        // 1 USD = 0.5 EUR, so a 50,000 EUR target is reached at 100,000 USD.
        val alert = alert(PriceAlertType.PRICE_ABOVE, "50000", currency = "EUR")
        assertFalse(PriceAlertEvaluator.isTriggered(alert, 99_999.0, 0.0, 0.5))
        assertTrue(PriceAlertEvaluator.isTriggered(alert, 100_000.0, 0.0, 0.5))
    }

    @Test
    fun `price alerts wait when the exchange rate or price is unknown`() {
        val above = alert(PriceAlertType.PRICE_ABOVE, "1")
        val below = alert(PriceAlertType.PRICE_BELOW, "1000000")
        assertFalse(PriceAlertEvaluator.isTriggered(above, 100.0, 0.0, null))
        assertFalse(PriceAlertEvaluator.isTriggered(below, 0.0, 0.0, 1.0))
        assertFalse(PriceAlertEvaluator.isTriggered(below, Double.NaN, 0.0, 1.0))
    }

    @Test
    fun `change alerts use the signed 24h percentage`() {
        val up = alert(PriceAlertType.CHANGE_UP, "5")
        val down = alert(PriceAlertType.CHANGE_DOWN, "5")
        assertTrue(PriceAlertEvaluator.isTriggered(up, 1.0, 5.0, 1.0))
        assertFalse(PriceAlertEvaluator.isTriggered(up, 1.0, 4.99, 1.0))
        assertFalse(PriceAlertEvaluator.isTriggered(up, 1.0, -9.0, 1.0))
        assertTrue(PriceAlertEvaluator.isTriggered(down, 1.0, -5.0, 1.0))
        assertFalse(PriceAlertEvaluator.isTriggered(down, 1.0, -4.99, 1.0))
        assertFalse(PriceAlertEvaluator.isTriggered(down, 1.0, 9.0, 1.0))
    }

    @Test
    fun `change alerts do not need a price or exchange rate`() {
        val up = alert(PriceAlertType.CHANGE_UP, "5")
        assertTrue(PriceAlertEvaluator.isTriggered(up, 0.0, 6.0, null))
    }

    @Test
    fun `non-positive or invalid thresholds never fire`() {
        assertFalse(PriceAlertEvaluator.isTriggered(alert(PriceAlertType.PRICE_ABOVE, "0"), 100.0, 0.0, 1.0))
        assertFalse(PriceAlertEvaluator.isTriggered(alert(PriceAlertType.CHANGE_UP, "-3"), 1.0, 10.0, 1.0))
        assertFalse(PriceAlertEvaluator.isTriggered(alert(PriceAlertType.CHANGE_UP, "5"), 1.0, Double.NaN, 1.0))
    }

    @Test
    fun `unknown stored type falls back to price above`() {
        assertEquals(PriceAlertType.PRICE_ABOVE, PriceAlertType.fromName("SOMETHING_ELSE"))
        assertEquals(PriceAlertType.CHANGE_DOWN, PriceAlertType.fromName("CHANGE_DOWN"))
    }
}
