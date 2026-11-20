package dev.arnoldsemase.calculator

import dev.arnoldsemase.calculator.engine.CurrencyConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyConverterTest {
    private val rates = mapOf("USD" to 1.0, "EUR" to 0.8, "ZAR" to 18.0, "UGX" to 3700.0)

    @Test fun convertsThroughTheBase() {
        assertEquals(1800.0, CurrencyConverter.convert(100.0, "USD", "ZAR", rates)!!, 1e-9)
        assertEquals(22.5, CurrencyConverter.convert(18.0, "ZAR", "EUR", rates)!!, 1e-9)
        assertEquals(5.0, CurrencyConverter.convert(5.0, "EUR", "EUR", rates)!!, 1e-9)
    }

    @Test fun missingCurrencyIsNull() {
        assertNull(CurrencyConverter.convert(1.0, "USD", "XXX", rates))
        assertNull(CurrencyConverter.convert(1.0, "XXX", "USD", rates))
    }

    @Test fun moneyFormatting() {
        assertEquals("1234.57", CurrencyConverter.moneyPlain(1234.5678))
        assertEquals("0", CurrencyConverter.moneyPlain(0.0))
        assertEquals("0.005432", CurrencyConverter.moneyPlain(0.0054321))
        assertEquals("-12.50", CurrencyConverter.moneyPlain(-12.5))
    }

    @Test fun rateFormatting() {
        assertEquals("18.424", CurrencyConverter.ratePlain(18.42367))
        assertEquals("3710", CurrencyConverter.ratePlain(3710.0))
        assertEquals("0.054276", CurrencyConverter.ratePlain(0.0542759))
    }

    @Test fun ageWording() {
        val now = 1_000_000_000L
        assertEquals("just now", CurrencyConverter.ago(now, now - 30_000))
        assertEquals("12 min ago", CurrencyConverter.ago(now, now - 12 * 60_000L))
        assertEquals("2 h ago", CurrencyConverter.ago(now, now - 2 * 3_600_000L))
        assertEquals("3 days ago", CurrencyConverter.ago(now, now - 3 * 86_400_000L))
    }
}
