package dev.arnoldsemase.calculator

import dev.arnoldsemase.calculator.engine.NumberFormatter
import dev.arnoldsemase.calculator.engine.UnitConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitConverterTest {

    private fun conv(category: String, value: Double, from: String, to: String): String {
        val units = UnitConverter.categories.first { it.name == category }.units
        val r = UnitConverter.convert(value, units.first { it.symbol == from }, units.first { it.symbol == to })
        return NumberFormatter.toPlain(r)
    }

    @Test fun length() {
        assertEquals("1.609344", conv("Length", 1.0, "mi", "km"))
        assertEquals("12", conv("Length", 1.0, "ft", "in"))
    }

    @Test fun marathonInMiles() {
        val miles = conv("Length", 42.195, "km", "mi").toDouble()
        assertEquals(26.2188, miles, 0.0001)
    }

    @Test fun mass() {
        assertEquals("453.59237", conv("Mass", 1.0, "lb", "g"))
        assertEquals("16", conv("Mass", 1.0, "lb", "oz"))
    }

    @Test fun temperature() {
        assertEquals("212", conv("Temp", 100.0, "°C", "°F"))
        assertEquals("-40", conv("Temp", -40.0, "°F", "°C"))
        assertEquals("-273.15", conv("Temp", 0.0, "K", "°C"))
        assertEquals("273.15", conv("Temp", 0.0, "°C", "K"))
    }

    @Test fun speedAndVolume() {
        assertEquals("1", conv("Speed", 3.6, "km/h", "m/s"))
        assertEquals("1000", conv("Volume", 1.0, "m³", "L"))
        assertEquals("3.785411784", conv("Volume", 1.0, "gal", "L"))
    }

    @Test fun everyCategoryRoundTrips() {
        for (cat in UnitConverter.categories) {
            assertTrue(cat.defaultFrom in cat.units.indices && cat.defaultTo in cat.units.indices)
            for (a in cat.units) for (b in cat.units) {
                val back = UnitConverter.convert(UnitConverter.convert(123.456, a, b), b, a)
                assertEquals("${a.symbol}→${b.symbol}", 123.456, back, 1e-6)
            }
        }
    }
}
