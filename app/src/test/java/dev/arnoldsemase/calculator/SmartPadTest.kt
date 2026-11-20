package dev.arnoldsemase.calculator

import dev.arnoldsemase.calculator.engine.GroupingStyle
import dev.arnoldsemase.calculator.engine.NumberFormatSettings
import dev.arnoldsemase.calculator.engine.NumberFormatter
import dev.arnoldsemase.calculator.engine.SmartPad
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartPadTest {

    private val fmt = NumberFormatter(NumberFormatSettings(grouping = GroupingStyle.NONE))
    private val rates = mapOf("USD" to 1.0, "ZAR" to 18.5, "EUR" to 0.92)

    private fun run(text: String, r: Map<String, Double>? = rates): List<String> =
        SmartPad.evaluate(text, r).map { v -> v?.let { SmartPad.format(it, fmt) } ?: "" }

    private fun one(line: String, r: Map<String, Double>? = rates) = run(line, r).single()

    @Test fun wordsAreIgnored() {
        assertEquals("155.25", one("3 coffees at 45 + 15% tip"))
        assertEquals("8500", one("Rent: 8500"))
        assertEquals("100", one("what is 20% of 500"))
    }

    @Test fun variables() {
        assertEquals(listOf("8500", "17000", "1700"), run("rent = 8500\nrent * 2\n20% of rent"))
        assertEquals(listOf("500", "1500"), run("monthly rent = 500\nmonthly rent * 3"))
        assertEquals(listOf("", "5"), run("rent * 2\n5"))
    }

    @Test fun percentages() {
        assertEquals("100", one("20% of 500"))
        assertEquals("115", one("100 + 15%"))
        assertEquals("85", one("100 - 15%"))
        assertEquals("15%", one("15%"))
        assertEquals("20", one("20 percent of 100"))
    }

    @Test fun totals() {
        assertEquals(listOf("8500", "3000", "11500", "1000", "12500"), run("rent = 8500\nfood 3000\ntotal\nfun 1000\ntotal"))
        assertEquals(listOf("100", "", "100"), run("100\n\ntotal"))
    }

    @Test fun conversions() {
        assertEquals("1850 ZAR", one("100 USD in ZAR"))
        assertEquals("3.10685596119 mi", one("5 km in miles"))
        assertEquals("212 °F", one("100 celsius to fahrenheit"))
        assertEquals("30.48 cm", one("12 in cm"))
        assertEquals("12 in", one("12 in"))
        assertEquals("5.3 km", one("5 km + 300 m"))
    }

    @Test fun ratesMissingIsBlankNotCrash() {
        assertEquals("", one("100 USD in ZAR", null))
        assertEquals("100 USD", one("100 USD in", null))
    }

    @Test fun incompleteLinesNeverCrash() {
        val partial = listOf("", "+", "3 +", "(2+3", "2 ^", "15%", "% of", "100 USD in", "rent =", "= 5", "((((", "))))", "5 /", "1/0",
            "-", "--5", "km", "in", "total", "ans", "3 coffees at", "1,5", "..", "1e999999", "x = x")
        for (l in partial) SmartPad.evaluate(l, rates)
        SmartPad.evaluate("(".repeat(50_000) + "1", rates)
        SmartPad.evaluate("-".repeat(50_000) + "1", rates)
        assertEquals("5", one("2 + 3"))
        assertEquals("", one("1/0"))
        assertEquals("5", one("(2+3"))
    }
}
