package dev.arnoldsemase.calculator

import dev.arnoldsemase.calculator.engine.AngleMode
import dev.arnoldsemase.calculator.engine.CalcError
import dev.arnoldsemase.calculator.engine.EvalResult
import dev.arnoldsemase.calculator.engine.ExpressionEvaluator
import dev.arnoldsemase.calculator.engine.NumberFormatSettings
import dev.arnoldsemase.calculator.engine.NumberFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpressionEvaluatorTest {

    private fun calc(expr: String, angle: AngleMode = AngleMode.DEG): String =
        when (val r = ExpressionEvaluator.evaluate(expr, angle)) {
            is EvalResult.Success -> NumberFormatter.toPlain(r.value)
            is EvalResult.Failure -> "ERR:${r.error}"
        }

    @Test fun precedence() {
        assertEquals("14", calc("2+3×4"))
        assertEquals("20", calc("(2+3)×4"))
        assertEquals("512", calc("2^3^2"))
        assertEquals("−4".replace('−', '-'), calc("−2^2"))
        assertEquals("5", calc("2−−3"))
    }

    @Test fun decimalsHaveNoFloatNoise() {
        assertEquals("0.3", calc("0.1+0.2"))
        assertEquals("1", calc("1÷3×3"))
    }

    @Test fun percent() {
        assertEquals("55", calc("50+10%"))
        assertEquals("45", calc("50−10%"))
        assertEquals("20", calc("200×10%"))
        assertEquals("0.5", calc("50%"))
    }

    @Test fun scientific() {
        assertEquals("0.5", calc("sin(30)"))
        assertEquals("1", calc("sin(π÷2)", AngleMode.RAD))
        assertEquals("0", calc("sin(180)"))
        assertEquals("1", calc("ln(e)"))
        assertEquals("2", calc("log(100)"))
        assertEquals("4", calc("√(16)"))
        assertEquals("120", calc("5!"))
        assertEquals("30", calc("asin(0.5)"))
        assertEquals("6.28318530718", calc("2π"))
    }

    @Test fun implicitMultiplicationAndAutoClose() {
        assertEquals("15", calc("3(4+1)"))
        assertEquals("4", calc("√(16"))
    }

    @Test fun errors() {
        assertEquals("ERR:${CalcError.DIVIDE_BY_ZERO}", calc("10÷0"))
        assertEquals("ERR:${CalcError.UNDEFINED}", calc("√(−4)"))
        assertEquals("ERR:${CalcError.UNDEFINED}", calc("tan(90)"))
        assertEquals("ERR:${CalcError.INVALID}", calc("2+×3".replace("+×", "×÷")))
        assertEquals("ERR:${CalcError.INVALID}", calc("()"))
        assertEquals("ERR:${CalcError.OVERFLOW}", calc("200!"))
    }

    @Test fun previewIgnoresTrailingOperators() {
        assertEquals(12.0, ExpressionEvaluator.preview("12+")!!, 0.0)
        assertEquals(2.0, ExpressionEvaluator.preview("2×sin(")!!, 0.0)
        assertNull(ExpressionEvaluator.preview("("))
    }

    @Test fun backspaceRemovesWholeFunction() {
        assertEquals("2+", ExpressionEvaluator.removeLastToken("2+sin("))
        assertEquals("2+", ExpressionEvaluator.removeLastToken("2+asin("))
        assertEquals("12", ExpressionEvaluator.removeLastToken("123"))
    }

    @Test fun formatting() {
        val f = NumberFormatter(NumberFormatSettings())
        assertEquals("1,234,567.89", f.display("1234567.89"))
        assertEquals("−1,000", f.display("-1000"))
        assertEquals("1E20".replace("E", "E"), f.display("100000000000000000000"))
        assertEquals("12.", f.typing("12."))
        val eu = NumberFormatter(NumberFormatSettings(decimalMark = dev.arnoldsemase.calculator.engine.DecimalMark.COMMA))
        assertEquals("1.234,5", eu.display("1234.5"))
    }
}
