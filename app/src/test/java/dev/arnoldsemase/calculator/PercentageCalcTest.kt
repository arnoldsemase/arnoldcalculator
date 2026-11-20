package dev.arnoldsemase.calculator

import dev.arnoldsemase.calculator.engine.PercentMode
import dev.arnoldsemase.calculator.engine.PercentageCalc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PercentageCalcTest {
    private fun calc(mode: PercentMode, a: Double, b: Double) = PercentageCalc.compute(mode, a, b)!!

    @Test fun percentOf() {
        val r = calc(PercentMode.OF, 15.0, 850.0)
        assertEquals(127.5, r.value, 1e-9)
        assertFalse(r.isPercent)
        assertEquals(977.5, r.lines[0].value, 1e-9)
        assertEquals(722.5, r.lines[1].value, 1e-9)
    }

    @Test fun percentChange() {
        val up = calc(PercentMode.CHANGE, 80.0, 100.0)
        assertEquals(25.0, up.value, 1e-9)
        assertTrue(up.isPercent)
        assertEquals(20.0, up.lines[0].value, 1e-9)
        assertEquals(-50.0, calc(PercentMode.CHANGE, 200.0, 100.0).value, 1e-9)
        assertEquals(50.0, calc(PercentMode.CHANGE, -10.0, -5.0).value, 1e-9)
    }

    @Test fun isWhatPercent() {
        val r = calc(PercentMode.IS, 30.0, 120.0)
        assertEquals(25.0, r.value, 1e-9)
        assertEquals(0.25, r.lines[0].value, 1e-9)
    }

    @Test fun tipAndDiscount() {
        val tip = calc(PercentMode.TIP, 15.0, 200.0)
        assertEquals(30.0, tip.value, 1e-9)
        assertEquals(230.0, tip.lines[0].value, 1e-9)
        val sale = calc(PercentMode.DISCOUNT, 20.0, 250.0)
        assertEquals(200.0, sale.value, 1e-9)
        assertEquals(50.0, sale.lines[0].value, 1e-9)
    }

    @Test fun missingOrImpossibleInputGivesNothing() {
        assertNull(PercentageCalc.compute(PercentMode.OF, null, 5.0))
        assertNull(PercentageCalc.compute(PercentMode.CHANGE, 0.0, 5.0))
        assertNull(PercentageCalc.compute(PercentMode.IS, 5.0, 0.0))
    }

    @Test fun plainText() {
        assertEquals("33.3333", PercentageCalc.plain(100.0 / 3.0))
        assertEquals("127.5", PercentageCalc.plain(127.5))
        assertEquals("0", PercentageCalc.plain(0.00001))
        assertEquals("-50", PercentageCalc.plain(-50.0))
    }
}
