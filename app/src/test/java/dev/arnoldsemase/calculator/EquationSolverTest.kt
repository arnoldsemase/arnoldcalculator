package dev.arnoldsemase.calculator

import dev.arnoldsemase.calculator.engine.EquationOutcome
import dev.arnoldsemase.calculator.engine.EquationSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EquationSolverTest {
    private fun solved(o: EquationOutcome) = o as EquationOutcome.Solved
    private fun exact(o: EquationOutcome) = solved(o).solutions.map { it.exact }

    // ---- Linear
    @Test fun linearBasics() {
        assertEquals(listOf("3"), exact(EquationSolver.linear("2x − 6 = 0")))
        assertEquals(listOf("3"), exact(EquationSolver.linear("3x + 2 = 11")))
        assertEquals(listOf("6"), exact(EquationSolver.linear("x/2 = 3")))
        assertEquals(listOf("6"), exact(EquationSolver.linear("2(x+1) = 3x − 4")))
        assertEquals(listOf("2"), exact(EquationSolver.linear("0.5x = 1")))
        assertEquals(listOf("0"), exact(EquationSolver.linear("x")))
    }

    @Test fun linearFractionsAndNegatives() {
        val r = solved(EquationSolver.linear("3x = 1")).solutions.single()
        assertEquals("1/3", r.exact)
        assertEquals("≈ 0.3333", r.approx)
        assertEquals(listOf("−4"), exact(EquationSolver.linear("x + 4 = 0")))
        assertEquals("= 0.5", solved(EquationSolver.linear("2x = 1")).solutions.single().approx)
    }

    @Test fun linearSpecialCases() {
        val none = EquationSolver.linear("x = x + 1") as EquationOutcome.NoUniqueSolution
        assertEquals("No solution", none.headline)
        val all = EquationSolver.linear("2x = 2x") as EquationOutcome.NoUniqueSolution
        assertEquals("Every x works", all.headline)
    }

    @Test fun linearRejectsTheWrongKindOfEquation() {
        assertTrue(EquationSolver.linear("x² = 4") is EquationOutcome.Problem)
        assertTrue(EquationSolver.linear("x + y = 2") is EquationOutcome.Problem)
    }

    // ---- Typing in progress and bad input
    @Test fun unfinishedInputWaits() {
        assertEquals(EquationOutcome.Waiting, EquationSolver.linear(""))
        assertEquals(EquationOutcome.Waiting, EquationSolver.linear("2x +"))
        assertEquals(EquationOutcome.Waiting, EquationSolver.linear("2x ="))
        assertEquals(EquationOutcome.Waiting, EquationSolver.linear("= 3"))
        assertEquals(EquationOutcome.Waiting, EquationSolver.quadratic("x²"  + " −"))
        assertEquals(EquationOutcome.Waiting, EquationSolver.quadratic("x^"))
    }

    @Test fun badInputIsAProblem() {
        assertTrue(EquationSolver.linear("2x $ 3") is EquationOutcome.Problem)
        assertTrue(EquationSolver.linear("2x = 3 = 4") is EquationOutcome.Problem)
        assertTrue(EquationSolver.linear("2x )") is EquationOutcome.Problem)
        assertTrue(EquationSolver.linear("1.2.3x = 1") is EquationOutcome.Problem)
        assertTrue(EquationSolver.linear("x / x = 1") is EquationOutcome.Problem)
        assertTrue(EquationSolver.linear("x / 0 = 1") is EquationOutcome.Problem)
        assertTrue(EquationSolver.quadratic("x^9 = 1") is EquationOutcome.Problem)
    }

    @Test fun missingClosingBracketIsForgiven() {
        assertEquals(listOf("6"), exact(EquationSolver.linear("2(x+1 = 3x − 4")))
    }

    // ---- Quadratic
    @Test fun quadraticTwoRationalRoots() {
        val r = solved(EquationSolver.quadratic("x² − 5x + 6 = 0"))
        assertEquals(listOf("3", "2"), r.solutions.map { it.exact })
        assertEquals(listOf("x₁", "x₂"), r.solutions.map { it.name })
        assertEquals("x = (5 ± 1) ÷ 2", r.steps.last().value)
        assertEquals("a = 1, b = −5, c = 6", r.steps[1].value)
        assertEquals(listOf("3/2", "1/3"), exact(EquationSolver.quadratic("6x² − 11x + 3 = 0")))
        assertEquals(listOf("2", "1/2"), exact(EquationSolver.quadratic("2x² − 5x + 2 = 0")))
    }

    @Test fun quadraticWithoutEqualsSignOrWithBrackets() {
        assertEquals(listOf("3", "2"), exact(EquationSolver.quadratic("x² − 5x + 6")))
        assertEquals(listOf("2", "1"), exact(EquationSolver.quadratic("(x − 1)(x − 2) = 0")))
        assertEquals(listOf("3", "−3"), exact(EquationSolver.quadratic("x^2 = 9")))
        assertEquals(listOf("3", "−3"), exact(EquationSolver.quadratic("x*x = 9")))
    }

    @Test fun quadraticDoubleRoot() {
        val r = solved(EquationSolver.quadratic("x² + 2x + 1 = 0"))
        assertEquals(listOf("−1"), r.solutions.map { it.exact })
        assertTrue(r.note.contains("double"))
    }

    @Test fun quadraticIrrationalRoots() {
        val r = solved(EquationSolver.quadratic("x² − 2 = 0"))
        assertEquals(listOf("√2", "−√2"), r.solutions.map { it.exact })
        assertEquals("≈ 1.4142", r.solutions[0].approx)
        assertEquals("≈ −1.4142", r.solutions[1].approx)
        assertEquals(listOf("(−1 + √5) ÷ 2", "(−1 − √5) ÷ 2"), exact(EquationSolver.quadratic("x² + x − 1 = 0")))
        assertEquals(listOf("2 + √3", "2 − √3"), exact(EquationSolver.quadratic("x² − 4x + 1 = 0")))
    }

    @Test fun quadraticComplexRoots() {
        val r = solved(EquationSolver.quadratic("x² + 1 = 0"))
        assertEquals(listOf("i", "−i"), r.solutions.map { it.exact })
        assertTrue(r.note.contains("complex"))
        assertNull(r.solutions[0].approx)
        assertEquals(listOf("(1 + i√3) ÷ 2", "(1 − i√3) ÷ 2"), exact(EquationSolver.quadratic("x² − x + 1 = 0")))
    }

    @Test fun quadraticNegativeLeadingCoefficientAndDecimals() {
        assertEquals(listOf("3", "2"), exact(EquationSolver.quadratic("−x² + 5x − 6 = 0")))
        assertEquals(listOf("2", "1"), exact(EquationSolver.quadratic("0.5x² − 1.5x + 1 = 0")))
    }

    @Test fun quadraticRejectsOtherDegrees() {
        assertTrue(EquationSolver.quadratic("2x + 1 = 0") is EquationOutcome.Problem)
        assertTrue(EquationSolver.quadratic("x³ = 1") is EquationOutcome.Problem)
        assertTrue(EquationSolver.quadratic("x² + y = 1") is EquationOutcome.Problem)
    }

    // ---- Systems
    @Test fun systemUniqueSolution() {
        val r = solved(EquationSolver.system("2x + y = 5", "x − y = 1"))
        assertEquals(listOf("2", "1"), r.solutions.map { it.exact })
        assertEquals(listOf("x", "y"), r.solutions.map { it.name })
    }

    @Test fun systemWithFractions() {
        assertEquals(listOf("2", "3"), exact(EquationSolver.system("x/2 + y/3 = 2", "x − y = −1")))
        assertEquals(listOf("1/2", "1/3"), exact(EquationSolver.system("2x = 1", "3y = 1")))
    }

    @Test fun systemSpecialCases() {
        val same = EquationSolver.system("x + y = 2", "2x + 2y = 4") as EquationOutcome.NoUniqueSolution
        assertEquals("Infinitely many solutions", same.headline)
        val parallel = EquationSolver.system("x + y = 2", "x + y = 3") as EquationOutcome.NoUniqueSolution
        assertEquals("No solution", parallel.headline)
        val impossible = EquationSolver.system("0 = 5", "0x = 0") as EquationOutcome.NoUniqueSolution
        assertEquals("No solution", impossible.headline)
    }

    @Test fun systemInputProblems() {
        assertEquals(EquationOutcome.Waiting, EquationSolver.system("x + y = 2", ""))
        assertEquals(EquationOutcome.Waiting, EquationSolver.system("x + y = 2", "x −"))
        assertTrue(EquationSolver.system("x² + y = 2", "x = 1") is EquationOutcome.Problem)
        assertTrue(EquationSolver.system("x*y = 2", "x = 1") is EquationOutcome.Problem)
    }
}
