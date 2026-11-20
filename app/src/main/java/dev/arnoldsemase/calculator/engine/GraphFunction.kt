package dev.arnoldsemase.calculator.engine

import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** A compiled f(x). Returns NaN or ±Infinity where the function is undefined. */
fun interface GraphFn {
    fun at(x: Double): Double
}

/**
 * Compiles text such as "sin(x)/x", "y = 2x^2 - 3" or "f(x)=√(x)" into a [GraphFn].
 * Separate from [ExpressionEvaluator] so the calculator itself is untouched.
 * Precedence: + −  <  × ÷ and implicit multiplication (2x, 3(x+1))  <  unary −  <  ^ (right-assoc).
 */
object GraphFunction {

    private val NAMES = listOf(
        "asin", "acos", "atan", "sqrt", "ceil", "floor",
        "sin", "cos", "tan", "abs", "exp", "log", "ln", "pi",
    )

    private class Bad : Exception()

    private fun bad(): Nothing = throw Bad()

    private sealed interface Tok {
        data class Num(val v: Double) : Tok
        data class Op(val c: Char) : Tok
        data class Func(val name: String) : Tok
        data class Const(val v: Double) : Tok
        data object X : Tok
        data object LP : Tok
        data object RP : Tok
    }

    /** Returns null when the text is empty or not a valid expression. */
    fun compile(source: String, angle: AngleMode = AngleMode.RAD): GraphFn? = try {
        var s = source.trim().lowercase()
        if ('=' in s) s = s.substringAfter('=')
        s = s.replace("²", "^2").replace("³", "^3")
        if (s.isBlank()) bad()
        val open = s.count { it == '(' } - s.count { it == ')' }
        if (open > 0) s += ")".repeat(open)
        val p = Parser(tokenize(s), angle)
        p.parse()
    } catch (e: Bad) {
        null
    }

    private fun tokenize(src: String): List<Tok> {
        val out = ArrayList<Tok>()
        var i = 0
        while (i < src.length) {
            val ch = src[i]
            when {
                ch.isWhitespace() -> i++
                ch in '0'..'9' || ch == '.' || ch == ',' -> {
                    var j = i
                    var dots = 0
                    while (j < src.length && (src[j] in '0'..'9' || src[j] == '.' || src[j] == ',')) {
                        if (src[j] == '.' || src[j] == ',') dots++
                        j++
                    }
                    val text = src.substring(i, j).replace(',', '.')
                    if (dots > 1 || text == ".") bad()
                    out.add(Tok.Num(text.toDouble()))
                    i = j
                }
                ch == '+' -> { out.add(Tok.Op('+')); i++ }
                ch == '-' || ch == '−' || ch == '–' -> { out.add(Tok.Op('-')); i++ }
                ch == '*' || ch == '×' || ch == '·' -> { out.add(Tok.Op('*')); i++ }
                ch == '/' || ch == '÷' -> { out.add(Tok.Op('/')); i++ }
                ch == '^' -> { out.add(Tok.Op('^')); i++ }
                ch == '(' -> { out.add(Tok.LP); i++ }
                ch == ')' -> { out.add(Tok.RP); i++ }
                ch == 'π' -> { out.add(Tok.Const(PI)); i++ }
                ch == '√' -> { out.add(Tok.Func("sqrt")); i++ }
                else -> {
                    val name = NAMES.firstOrNull { src.startsWith(it, i) }
                    when {
                        name == "pi" -> { out.add(Tok.Const(PI)); i += 2 }
                        name != null -> { out.add(Tok.Func(name)); i += name.length }
                        ch == 'x' -> { out.add(Tok.X); i++ }
                        ch == 'e' -> { out.add(Tok.Const(E)); i++ }
                        else -> bad()
                    }
                }
            }
        }
        return out
    }

    private class Parser(private val t: List<Tok>, private val angle: AngleMode) {
        private var p = 0

        fun parse(): GraphFn {
            if (t.isEmpty()) bad()
            val f = expr()
            if (p != t.size) bad()
            return f
        }

        private fun peek(): Tok? = t.getOrNull(p)

        private fun peekOp(vararg cs: Char): Boolean {
            val tok = peek()
            return tok is Tok.Op && tok.c in cs
        }

        private fun startsPrimary(): Boolean = when (peek()) {
            is Tok.Num, Tok.LP, is Tok.Func, is Tok.Const, Tok.X -> true
            else -> false
        }

        private fun expr(): GraphFn {
            var left = term()
            while (peekOp('+', '-')) {
                val plus = (t[p++] as Tok.Op).c == '+'
                val a = left
                val b = term()
                left = if (plus) GraphFn { a.at(it) + b.at(it) } else GraphFn { a.at(it) - b.at(it) }
            }
            return left
        }

        private fun term(): GraphFn {
            var left = unary()
            while (true) {
                if (peekOp('*', '/')) {
                    val mul = (t[p++] as Tok.Op).c == '*'
                    val a = left
                    val b = unary()
                    left = if (mul) GraphFn { a.at(it) * b.at(it) } else GraphFn { a.at(it) / b.at(it) }
                } else if (startsPrimary()) {
                    val a = left
                    val b = power() // implicit multiplication: 2x, 3(x+1), x(x-1)
                    left = GraphFn { a.at(it) * b.at(it) }
                } else {
                    return left
                }
            }
        }

        private fun unary(): GraphFn {
            if (peekOp('-')) { p++; val a = unary(); return GraphFn { -a.at(it) } }
            if (peekOp('+')) { p++; return unary() }
            return power()
        }

        private fun power(): GraphFn {
            val base = primary()
            if (peekOp('^')) {
                p++
                val exponent = unary()
                return GraphFn { base.at(it).pow(exponent.at(it)) }
            }
            return base
        }

        private fun primary(): GraphFn {
            when (val tok = peek()) {
                is Tok.Num -> { p++; val v = tok.v; return GraphFn { v } }
                is Tok.Const -> { p++; val v = tok.v; return GraphFn { v } }
                Tok.X -> { p++; return GraphFn { it } }
                Tok.LP -> {
                    p++
                    val inner = expr()
                    if (peek() != Tok.RP) bad()
                    p++
                    return inner
                }
                is Tok.Func -> {
                    p++
                    val arg: GraphFn
                    if (peek() == Tok.LP) {
                        p++
                        arg = expr()
                        if (peek() != Tok.RP) bad()
                        p++
                    } else {
                        arg = power() // "sin x" works too
                    }
                    return apply(tok.name, arg)
                }
                else -> bad()
            }
        }

        private fun toRad(x: Double) = if (angle == AngleMode.DEG) Math.toRadians(x) else x
        private fun fromRad(x: Double) = if (angle == AngleMode.DEG) Math.toDegrees(x) else x

        private fun apply(name: String, a: GraphFn): GraphFn = when (name) {
            "sin" -> GraphFn { sin(toRad(a.at(it))) }
            "cos" -> GraphFn { cos(toRad(a.at(it))) }
            "tan" -> GraphFn { tan(toRad(a.at(it))) }
            "asin" -> GraphFn { fromRad(asin(a.at(it))) }
            "acos" -> GraphFn { fromRad(acos(a.at(it))) }
            "atan" -> GraphFn { fromRad(atan(a.at(it))) }
            "ln" -> GraphFn { ln(a.at(it)) }
            "log" -> GraphFn { log10(a.at(it)) }
            "sqrt" -> GraphFn { sqrt(a.at(it)) }
            "abs" -> GraphFn { abs(a.at(it)) }
            "exp" -> GraphFn { exp(a.at(it)) }
            "floor" -> GraphFn { floor(a.at(it)) }
            "ceil" -> GraphFn { ceil(a.at(it)) }
            else -> bad()
        }
    }
}
