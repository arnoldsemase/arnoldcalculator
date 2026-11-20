package dev.arnoldsemase.calculator.engine

import java.math.BigInteger

// ---- Results -----------------------------------------------------------------------------------------

/** One answer, such as `x₁ = (5 + √13) ÷ 2`, with an optional decimal form like "≈ 4.3028". */
data class Solution(val name: String, val exact: String, val approx: String? = null)

data class WorkingStep(val label: String, val value: String)

sealed interface EquationOutcome {
    data class Solved(
        val solutions: List<Solution>,
        /** A short remark such as "Two real roots" or "Complex roots". */
        val note: String,
        val steps: List<WorkingStep>,
    ) : EquationOutcome

    /** The equation is fine but has no single answer: no solution, or every value works. */
    data class NoUniqueSolution(val headline: String, val detail: String, val steps: List<WorkingStep>) : EquationOutcome

    /** Nothing to solve yet: empty, or the equation is still being typed. */
    data object Waiting : EquationOutcome

    data class Problem(val message: String) : EquationOutcome
}

// ---- Polynomials with exact coefficients ---------------------------------------------------------------

private data class Mono(val x: Int, val y: Int)

private val ZERO = Fraction.ofInt(BigInteger.ZERO)
private val Fraction.isZero: Boolean get() = num.signum() == 0

private class Poly(terms: Map<Mono, Fraction>) {
    val terms: Map<Mono, Fraction> = terms.filterValues { !it.isZero }

    operator fun plus(o: Poly): Poly {
        val m = HashMap(terms)
        for ((k, v) in o.terms) m[k] = (m[k] ?: ZERO) + v
        return Poly(m)
    }

    operator fun unaryMinus(): Poly = Poly(terms.mapValues { ZERO - it.value })
    operator fun minus(o: Poly): Poly = this + (-o)

    operator fun times(o: Poly): Poly {
        val m = HashMap<Mono, Fraction>()
        for ((a, p) in terms) for ((b, q) in o.terms) {
            val k = Mono(a.x + b.x, a.y + b.y)
            m[k] = (m[k] ?: ZERO) + p * q
        }
        return Poly(m)
    }

    val isConstant: Boolean get() = terms.keys.all { it.x == 0 && it.y == 0 }
    fun coefficient(x: Int, y: Int): Fraction = terms[Mono(x, y)] ?: ZERO
    val maxX: Int get() = terms.keys.maxOfOrNull { it.x } ?: 0
    val maxY: Int get() = terms.keys.maxOfOrNull { it.y } ?: 0
    val maxTotal: Int get() = terms.keys.maxOfOrNull { it.x + it.y } ?: 0

    companion object {
        fun constant(f: Fraction) = Poly(mapOf(Mono(0, 0) to f))
        val ONE = constant(Fraction.ofInt(BigInteger.ONE))
        val X = Poly(mapOf(Mono(1, 0) to Fraction.ofInt(BigInteger.ONE)))
        val Y = Poly(mapOf(Mono(0, 1) to Fraction.ofInt(BigInteger.ONE)))
    }
}

// ---- Reading the text ----------------------------------------------------------------------------------

private class EquationException(message: String, val incomplete: Boolean = false) : Exception(message)

private sealed interface Tok {
    data class Num(val value: Fraction) : Tok
    data class Var(val name: Char) : Tok
    data class Op(val c: Char) : Tok
    data object LParen : Tok
    data object RParen : Tok
    data object Equals : Tok
}

private fun tokenize(text: String): List<Tok> {
    val out = ArrayList<Tok>()
    var i = 0
    while (i < text.length) {
        val ch = text[i]
        when {
            ch.isWhitespace() || ch == ' ' -> i++
            ch in '0'..'9' || ch == '.' -> {
                var j = i
                while (j < text.length && (text[j] in '0'..'9' || text[j] == '.')) j++
                val bd = text.substring(i, j).toBigDecimalOrNull()
                    ?: throw EquationException("That number doesn't look right")
                val scale = bd.scale().coerceAtLeast(0)
                out += Tok.Num(Fraction.of(bd.movePointRight(scale).toBigIntegerExact(), BigInteger.TEN.pow(scale))!!)
                i = j
            }
            ch == 'x' || ch == 'X' -> { out += Tok.Var('x'); i++ }
            ch == 'y' || ch == 'Y' -> { out += Tok.Var('y'); i++ }
            ch == '+' -> { out += Tok.Op('+'); i++ }
            ch == '−' || ch == '-' || ch == '–' -> { out += Tok.Op('-'); i++ }
            ch == '×' || ch == '*' || ch == '·' -> { out += Tok.Op('*'); i++ }
            ch == '÷' || ch == '/' -> { out += Tok.Op('/'); i++ }
            ch == '^' -> { out += Tok.Op('^'); i++ }
            ch == '²' -> { out += Tok.Op('^'); out += Tok.Num(Fraction.ofInt(BigInteger.valueOf(2))); i++ }
            ch == '³' -> { out += Tok.Op('^'); out += Tok.Num(Fraction.ofInt(BigInteger.valueOf(3))); i++ }
            ch == '(' -> { out += Tok.LParen; i++ }
            ch == ')' -> { out += Tok.RParen; i++ }
            ch == '=' -> { out += Tok.Equals; i++ }
            else -> throw EquationException("I don't understand \"$ch\"")
        }
    }
    return out
}

private class Parser(private val t: List<Tok>) {
    private var p = 0
    private fun peek(): Tok? = t.getOrNull(p)

    fun parseAll(): Poly {
        val v = expr()
        if (p < t.size) throw EquationException("Check the brackets and signs")
        return v
    }

    private fun expr(): Poly {
        var left = term()
        while (true) {
            val op = peek()
            if (op is Tok.Op && (op.c == '+' || op.c == '-')) {
                p++
                val right = term()
                left = if (op.c == '+') left + right else left - right
            } else return left
        }
    }

    private fun startsPrimary(tok: Tok?) = tok is Tok.Num || tok is Tok.Var || tok is Tok.LParen

    private fun term(): Poly {
        var left = unary()
        while (true) {
            val nx = peek()
            when {
                nx is Tok.Op && nx.c == '*' -> { p++; left *= unary() }
                nx is Tok.Op && nx.c == '/' -> {
                    p++
                    val d = unary()
                    if (!d.isConstant) throw EquationException("I can only divide by a number")
                    val v = d.coefficient(0, 0)
                    val inverse = Fraction.of(v.den, v.num) ?: throw EquationException("Can't divide by zero")
                    left *= Poly.constant(inverse)
                }
                startsPrimary(nx) -> left *= unary() // 2x, 3(x + 1), (x + 1)(x − 2)
                else -> return left
            }
        }
    }

    private fun unary(): Poly {
        val nx = peek()
        if (nx is Tok.Op && nx.c == '-') { p++; return -unary() }
        if (nx is Tok.Op && nx.c == '+') { p++; return unary() }
        return power()
    }

    private fun power(): Poly {
        val base = primary()
        val nx = peek()
        if (nx is Tok.Op && nx.c == '^') {
            p++
            val exp = exponent()
            var r = Poly.ONE
            repeat(exp) { r *= base }
            val after = peek()
            if (after is Tok.Op && after.c == '^') throw EquationException("Use brackets for a power of a power")
            return r
        }
        return base
    }

    private fun exponent(): Int {
        var tok = peek() ?: throw EquationException("", incomplete = true)
        var bracketed = false
        if (tok is Tok.LParen) {
            bracketed = true
            p++
            tok = peek() ?: throw EquationException("", incomplete = true)
        }
        if (tok !is Tok.Num || !tok.value.isInteger) throw EquationException("Powers must be whole numbers")
        p++
        if (bracketed) {
            if (peek() is Tok.RParen) p++ else if (peek() != null) throw EquationException("Check the brackets")
        }
        val n = tok.value.num
        if (n.signum() < 0 || n > BigInteger.valueOf(4)) throw EquationException("Powers from 0 to 4 are supported")
        return n.toInt()
    }

    private fun primary(): Poly {
        val tok = peek() ?: throw EquationException("", incomplete = true)
        p++
        return when (tok) {
            is Tok.Num -> Poly.constant(tok.value)
            is Tok.Var -> if (tok.name == 'x') Poly.X else Poly.Y
            Tok.LParen -> {
                val inner = expr()
                if (peek() is Tok.RParen) p++ else if (peek() != null) throw EquationException("Check the brackets")
                inner // a missing ")" at the very end is closed for the user
            }
            else -> throw EquationException("Check the signs and brackets")
        }
    }
}

/** Parses "left = right" (or just an expression, meaning "= 0") into left − right. */
private fun parseEquation(text: String): Poly {
    val tokens = tokenize(text)
    val eq = tokens.indexOfFirst { it is Tok.Equals }
    if (tokens.count { it is Tok.Equals } > 1) throw EquationException("Only one \"=\" is allowed")
    if (eq < 0) {
        if (tokens.isEmpty()) throw EquationException("", incomplete = true)
        return Parser(tokens).parseAll()
    }
    val left = tokens.subList(0, eq)
    val right = tokens.subList(eq + 1, tokens.size)
    if (left.isEmpty() || right.isEmpty()) throw EquationException("", incomplete = true)
    return Parser(left).parseAll() - Parser(right).parseAll()
}

// ---- Number helpers ------------------------------------------------------------------------------------

private fun show(v: BigInteger): String = v.toString().replace('-', '−')
private fun show(f: Fraction): String = f.improper().replace('-', '−')

private fun approxOf(f: Fraction): String? =
    if (f.isInteger) null else (if (f.isTerminating) "= " else "≈ ") + f.decimalPlain().replace('-', '−')

private fun approxOf(d: Double): String = "≈ " + PercentageCalc.plain(d).replace('-', '−')

private fun lcm(a: BigInteger, b: BigInteger): BigInteger = a / a.gcd(b) * b

/** Scales the coefficients to whole numbers with no common factor (the order and signs are kept). */
private fun integers(values: List<Fraction>): List<BigInteger> {
    val l = values.fold(BigInteger.ONE) { acc, f -> lcm(acc, f.den) }
    val scaled = values.map { it.num * (l / it.den) }
    val g = scaled.fold(BigInteger.ZERO) { acc, v -> acc.gcd(v) }
    return if (g.signum() == 0) scaled else scaled.map { it / g }
}

/** Integer square root by Newton's method (BigInteger.sqrt() needs a newer Android than we support). */
private fun isqrt(n: BigInteger): BigInteger {
    if (n.signum() <= 0) return BigInteger.ZERO
    if (n < BigInteger.valueOf(4)) return BigInteger.ONE
    var x = BigInteger.ONE.shiftLeft((n.bitLength() + 1) / 2)
    while (true) {
        val y = (x + n / x).shiftRight(1)
        if (y >= x) return x
        x = y
    }
}

/** Splits n into k²·m with m as small as trial division finds. */
private fun squareFree(n: BigInteger): Pair<BigInteger, BigInteger> {
    var rest = n
    var k = BigInteger.ONE
    var i = BigInteger.valueOf(2)
    while (i * i <= rest && i < BigInteger.valueOf(100_000)) {
        val sq = i * i
        while (rest.mod(sq).signum() == 0) {
            rest /= sq
            k *= i
        }
        i += BigInteger.ONE
    }
    return k to rest
}

/** "x² − 5x + 6" style text for a polynomial with whole coefficients, highest power first. */
private fun polynomialText(coefficients: List<BigInteger>, vararg symbols: String): String {
    val sb = StringBuilder()
    coefficients.forEachIndexed { i, c ->
        if (c.signum() == 0) return@forEachIndexed
        val symbol = symbols[i]
        val abs = c.abs()
        val body = if (symbol.isEmpty()) abs.toString() else if (abs == BigInteger.ONE) symbol else "$abs$symbol"
        if (sb.isEmpty()) {
            sb.append(if (c.signum() < 0) "−" else "").append(body)
        } else {
            sb.append(if (c.signum() < 0) " − " else " + ").append(body)
        }
    }
    return if (sb.isEmpty()) "0" else sb.toString()
}

/** (−b ± k√m) ÷ 2a in lowest terms. [sign] is "+", "−" or "±". */
private fun surdText(b: BigInteger, disc: BigInteger, twoA: BigInteger, sign: String, imaginary: Boolean): String {
    val (k, m) = squareFree(disc.abs())
    val g = b.abs().gcd(k).gcd(twoA).let { if (it.signum() == 0) BigInteger.ONE else it }
    val head = b.negate() / g
    val kk = k / g
    val d = twoA / g
    val radical = if (m == BigInteger.ONE) "" else "√$m"
    val coef = if (kk == BigInteger.ONE && (radical.isNotEmpty() || imaginary)) "" else kk.toString()
    val root = coef + (if (imaginary) "i" else "") + radical
    val top = if (head.signum() == 0) {
        when (sign) { "−" -> "−$root"; "±" -> "±$root"; else -> root }
    } else {
        "${show(head)} $sign $root"
    }
    return when {
        d == BigInteger.ONE -> top
        head.signum() == 0 -> "$top ÷ $d"
        else -> "($top) ÷ $d"
    }
}

// ---- Solvers -------------------------------------------------------------------------------------------

object EquationSolver {

    private inline fun guarded(text: String, block: (Poly) -> EquationOutcome): EquationOutcome {
        if (text.isBlank()) return EquationOutcome.Waiting
        val poly = try {
            parseEquation(text)
        } catch (e: EquationException) {
            return if (e.incomplete) EquationOutcome.Waiting else EquationOutcome.Problem(e.message ?: "Can't read that")
        }
        return block(poly)
    }

    fun linear(text: String): EquationOutcome = guarded(text) { p ->
        when {
            p.maxY > 0 -> EquationOutcome.Problem("That has x and y. Try the System tab.")
            p.maxX > 1 -> EquationOutcome.Problem("That has a power of x. Try the Quadratic tab.")
            else -> solveLinear(p)
        }
    }

    private fun solveLinear(p: Poly): EquationOutcome {
        val (a, b) = integers(listOf(p.coefficient(1, 0), p.coefficient(0, 0)))
        val form = "${polynomialText(listOf(a, b), "x", "")} = 0"
        val steps = mutableListOf(WorkingStep("Move everything to one side", form))
        if (a.signum() == 0) {
            return if (b.signum() == 0) {
                EquationOutcome.NoUniqueSolution("Every x works", "Both sides are always equal.", steps)
            } else {
                EquationOutcome.NoUniqueSolution("No solution", "The two sides can never be equal.", steps)
            }
        }
        val x = Fraction.of(b.negate(), a)!!
        steps += WorkingStep("Isolate x", "x = ${show(b.negate())} ÷ ${show(a)}")
        return EquationOutcome.Solved(
            listOf(Solution("x", show(x), approxOf(x))),
            "One solution",
            steps,
        )
    }

    fun quadratic(text: String): EquationOutcome = guarded(text) { p ->
        when {
            p.maxY > 0 -> EquationOutcome.Problem("That has x and y. Try the System tab.")
            p.maxX > 2 -> EquationOutcome.Problem("Powers above x² aren't supported.")
            p.maxX < 2 -> EquationOutcome.Problem("There's no x² term. Try the Linear tab.")
            else -> solveQuadratic(p)
        }
    }

    private fun solveQuadratic(p: Poly): EquationOutcome {
        var (a, b, c) = integers(listOf(p.coefficient(2, 0), p.coefficient(1, 0), p.coefficient(0, 0)))
        if (a.signum() < 0) { a = a.negate(); b = b.negate(); c = c.negate() }
        val disc = b * b - BigInteger.valueOf(4) * a * c
        val twoA = a * BigInteger.valueOf(2)
        val steps = mutableListOf(
            WorkingStep("Standard form", "${polynomialText(listOf(a, b, c), "x²", "x", "")} = 0"),
            WorkingStep("Coefficients", "a = ${show(a)}, b = ${show(b)}, c = ${show(c)}"),
            WorkingStep("Discriminant", "b² − 4ac = ${show(disc)}"),
        )
        val negB = b.negate()
        when {
            disc.signum() > 0 -> {
                val s = isqrt(disc)
                if (s * s == disc) {
                    steps += WorkingStep("Formula", "x = (${show(negB)} ± $s) ÷ ${show(twoA)}")
                    val x1 = Fraction.of(negB + s, twoA)!!
                    val x2 = Fraction.of(negB - s, twoA)!!
                    return EquationOutcome.Solved(
                        listOf(Solution("x₁", show(x1), approxOf(x1)), Solution("x₂", show(x2), approxOf(x2))),
                        "Two real solutions",
                        steps,
                    )
                }
                steps += WorkingStep("Formula", "x = ${surdText(b, disc, twoA, "±", false)}")
                val root = Math.sqrt(disc.toDouble())
                val d = twoA.toDouble()
                return EquationOutcome.Solved(
                    listOf(
                        Solution("x₁", surdText(b, disc, twoA, "+", false), approxOf((negB.toDouble() + root) / d)),
                        Solution("x₂", surdText(b, disc, twoA, "−", false), approxOf((negB.toDouble() - root) / d)),
                    ),
                    "Two real solutions",
                    steps,
                )
            }
            disc.signum() == 0 -> {
                val x = Fraction.of(negB, twoA)!!
                steps += WorkingStep("Formula", "x = ${show(negB)} ÷ ${show(twoA)}")
                return EquationOutcome.Solved(
                    listOf(Solution("x", show(x), approxOf(x))),
                    "One solution (a double root)",
                    steps,
                )
            }
            else -> {
                steps += WorkingStep("Formula", "x = ${surdText(b, disc, twoA, "±", true)}")
                return EquationOutcome.Solved(
                    listOf(
                        Solution("x₁", surdText(b, disc, twoA, "+", true)),
                        Solution("x₂", surdText(b, disc, twoA, "−", true)),
                    ),
                    "No real solutions: the roots are complex",
                    steps,
                )
            }
        }
    }

    fun system(first: String, second: String): EquationOutcome {
        if (first.isBlank() || second.isBlank()) return EquationOutcome.Waiting
        val (p1, p2) = try {
            parseEquation(first) to parseEquation(second)
        } catch (e: EquationException) {
            return if (e.incomplete) EquationOutcome.Waiting else EquationOutcome.Problem(e.message ?: "Can't read that")
        }
        if (p1.maxTotal > 1 || p2.maxTotal > 1) {
            return EquationOutcome.Problem("Each equation must be a straight line in x and y.")
        }
        // a·x + b·y + c = 0  becomes  a·x + b·y = −c
        fun row(p: Poly) = integers(listOf(p.coefficient(1, 0), p.coefficient(0, 1), ZERO - p.coefficient(0, 0)))
        val (a1, b1, c1) = row(p1)
        val (a2, b2, c2) = row(p2)
        val steps = mutableListOf(
            WorkingStep("Equation 1", "${polynomialText(listOf(a1, b1), "x", "y")} = ${show(c1)}"),
            WorkingStep("Equation 2", "${polynomialText(listOf(a2, b2), "x", "y")} = ${show(c2)}"),
        )
        // An equation like 0 = 5 can never hold.
        if ((a1.signum() == 0 && b1.signum() == 0 && c1.signum() != 0) ||
            (a2.signum() == 0 && b2.signum() == 0 && c2.signum() != 0)
        ) {
            return EquationOutcome.NoUniqueSolution("No solution", "One equation can never be true.", steps)
        }
        val d = a1 * b2 - a2 * b1
        val dx = c1 * b2 - c2 * b1
        val dy = a1 * c2 - a2 * c1
        steps += WorkingStep("Determinant", "D = ${show(a1)}·${show(b2)} − ${show(a2)}·${show(b1)} = ${show(d)}")
        if (d.signum() == 0) {
            return if (dx.signum() == 0 && dy.signum() == 0) {
                EquationOutcome.NoUniqueSolution("Infinitely many solutions", "The two equations describe the same line.", steps)
            } else {
                EquationOutcome.NoUniqueSolution("No solution", "The lines are parallel and never meet.", steps)
            }
        }
        val x = Fraction.of(dx, d)!!
        val y = Fraction.of(dy, d)!!
        steps += WorkingStep("Solve", "x = ${show(dx)} ÷ ${show(d)},  y = ${show(dy)} ÷ ${show(d)}")
        return EquationOutcome.Solved(
            listOf(Solution("x", show(x), approxOf(x)), Solution("y", show(y), approxOf(y))),
            "One solution",
            steps,
        )
    }
}
