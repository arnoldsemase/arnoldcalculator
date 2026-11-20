package dev.arnoldsemase.calculator

import dev.arnoldsemase.calculator.engine.Fraction
import dev.arnoldsemase.calculator.engine.FractionCalc
import dev.arnoldsemase.calculator.engine.FractionInput
import dev.arnoldsemase.calculator.engine.FractionOp
import dev.arnoldsemase.calculator.engine.FractionOutcome
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FractionsTest {
    private fun f(n: Long, d: Long) = Fraction.of(BigInteger.valueOf(n), BigInteger.valueOf(d))!!
    private fun input(num: String, den: String, whole: String = "", negative: Boolean = false) =
        FractionInput(negative = negative, mixed = whole.isNotEmpty(), whole = whole, num = num, den = den)

    private fun answer(a: FractionInput, op: FractionOp, b: FractionInput) =
        FractionCalc.compute(a, op, b) as FractionOutcome.Answer

    @Test fun alwaysLowestTermsWithPositiveDenominator() {
        assertEquals(f(1, 2), f(6, 12))
        assertEquals("-3/4", f(3, -4).improper())
        assertEquals("0", f(0, 5).improper())
        assertNull(Fraction.of(BigInteger.ONE, BigInteger.ZERO))
    }

    @Test fun addWithCommonDenominator() {
        val r = answer(input("3", "4"), FractionOp.PLUS, input("5", "6"))
        assertEquals("19/12", r.value.improper())
        assertEquals("1 7/12", r.value.mixedText())
        assertEquals("LCD 12 → 9/12 + 10/12", r.step)
        assertNull(r.unreduced)
        assertEquals("1.5833", r.value.decimalPlain())
    }

    @Test fun subtractGoesNegative() {
        val r = answer(input("1", "2"), FractionOp.MINUS, input("3", "4"))
        assertEquals("-1/4", r.value.improper())
        assertEquals("-1/4", r.value.mixedText())
        assertEquals("LCD 4 → 2/4 − 3/4", r.step)
        assertNull(r.unreduced)
    }

    @Test fun multiplyReportsWhatItSimplifiedFrom() {
        val r = answer(input("2", "3"), FractionOp.TIMES, input("3", "4"))
        assertEquals("1/2", r.value.improper())
        assertEquals("6/12", r.unreduced)
    }

    @Test fun divideFlipsTheSecond() {
        val r = answer(input("1", "2"), FractionOp.DIVIDE, input("1", "4"))
        assertEquals("2", r.value.improper())
        assertTrue(r.value.isInteger)
        assertEquals("1/2 × 4 (flip the second)", r.step)
    }

    @Test fun mixedNumbersAndSigns() {
        val r = answer(input("1", "2", whole = "1"), FractionOp.PLUS, input("1", "2"))
        assertEquals("2", r.value.improper())
        val neg = answer(input("1", "2", whole = "2", negative = true), FractionOp.PLUS, input("1", "2"))
        assertEquals("-2", neg.value.improper())
        assertEquals("-2 1/2", (f(-5, 2)).mixedText())
    }

    @Test fun wholeNumbersAlone() {
        val r = answer(FractionInput(whole = "3", mixed = true), FractionOp.TIMES, input("1", "3"))
        assertEquals("1", r.value.improper())
    }

    @Test fun incompleteAndInvalidInput() {
        assertEquals(FractionOutcome.Waiting, FractionCalc.compute(input("3", ""), FractionOp.PLUS, input("1", "2")))
        assertEquals(FractionOutcome.Waiting, FractionCalc.compute(input("", ""), FractionOp.PLUS, input("1", "2")))
        assertTrue(FractionCalc.compute(input("3", "0"), FractionOp.PLUS, input("1", "2")) is FractionOutcome.Problem)
        assertTrue(FractionCalc.compute(input("1", "2"), FractionOp.DIVIDE, input("0", "5")) is FractionOutcome.Problem)
    }

    @Test fun decimals() {
        assertEquals("0.75", f(3, 4).decimalPlain())
        assertTrue(f(3, 4).isTerminating)
        assertEquals("0.3333", f(1, 3).decimalPlain())
        assertFalse(f(1, 3).isTerminating)
    }

    @Test fun hugeNumbersStayExact() {
        val r = answer(input("999999", "1000000"), FractionOp.TIMES, input("999999", "1000000"))
        assertEquals("999998000001/1000000000000", r.value.improper())
    }
}
