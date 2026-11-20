package dev.arnoldsemase.calculator

import dev.arnoldsemase.calculator.engine.AngleMode
import dev.arnoldsemase.calculator.engine.GraphFunction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphFunctionTest {
    private fun y(src: String, x: Double, angle: AngleMode = AngleMode.RAD) =
        GraphFunction.compile(src, angle)!!.at(x)

    @Test fun basicPolynomials() {
        assertEquals(9.0, y("x^2", 3.0), 1e-9)
        assertEquals(-9.0, y("-x^2", 3.0), 1e-9)
        assertEquals(7.0, y("2x+1", 3.0), 1e-9)
        assertEquals(8.0, y("y = x^3", 2.0), 1e-9)
        assertEquals(4.0, y("f(x)=x²", 2.0), 1e-9)
    }

    @Test fun implicitMultiplicationAndConstants() {
        assertEquals(8.0, y("2(x+1)", 3.0), 1e-9)
        assertEquals(12.0, y("x(x+1)", 3.0), 1e-9)
        assertEquals(2 * Math.PI, y("2π", 0.0), 1e-9)
        assertEquals(Math.E, y("e", 0.0), 1e-9)
    }

    @Test fun functions() {
        assertEquals(0.0, y("sin(x)", 0.0), 1e-9)
        assertEquals(1.0, y("sin(x)", 90.0, AngleMode.DEG), 1e-9)
        assertEquals(3.0, y("√(x)", 9.0), 1e-9)
        assertEquals(2.0, y("abs(x)", -2.0), 1e-9)
        assertEquals(1.0, y("ln(x)", Math.E), 1e-9)
        assertEquals(2.0, y("log(x)", 100.0), 1e-9)
        assertEquals(Math.sin(2.0), y("sin x", 2.0), 1e-9)
    }

    @Test fun undefinedPointsAreNotFinite() {
        assertTrue(y("1/x", 0.0).isInfinite())
        assertTrue(y("sqrt(x)", -1.0).isNaN())
        assertTrue(y("ln(x)", -1.0).isNaN())
    }

    @Test fun missingParenthesesAreClosed() {
        assertEquals(Math.sin(1.0), y("sin(x", 1.0), 1e-9)
    }

    @Test fun invalidInput() {
        assertNull(GraphFunction.compile(""))
        assertNull(GraphFunction.compile("2 +"))
        assertNull(GraphFunction.compile("foo(x)"))
        assertNull(GraphFunction.compile("x +* 2"))
        assertNotNull(GraphFunction.compile("x"))
    }
}
