package dev.arnoldsemase.calculator.engine

import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class AngleMode { DEG, RAD }

enum class CalcError(val message: String) {
    DIVIDE_BY_ZERO("Can't divide by zero"),
    INVALID("Invalid expression"),
    UNDEFINED("Result is undefined"),
    OVERFLOW("Number is too large"),
}

sealed interface EvalResult {
    data class Success(val value: Double) : EvalResult
    data class Failure(val error: CalcError) : EvalResult
}

private class CalcException(val error: CalcError) : Exception(error.message)

private fun fail(error: CalcError = CalcError.INVALID): Nothing = throw CalcException(error)

private sealed interface Token {
    data class Num(val value: Double) : Token
    data class Op(val symbol: Char) : Token
    data class Func(val name: String) : Token
    data class Const(val value: Double) : Token
    data object LParen : Token
    data object RParen : Token
    data object Percent : Token
    data object Factorial : Token
}

/**
 * Recursive-descent evaluator with standard precedence:
 *   + −  <  × ÷ (and implicit multiplication)  <  unary −  <  ^ (right-assoc)  <  % !  <  functions / parentheses
 * A trailing percent after + or − is relative to the left operand (50 + 10% = 55).
 */
object ExpressionEvaluator {

    private val FUNCTION_NAMES = listOf("asin", "acos", "atan", "sin", "cos", "tan", "log", "ln", "√")
    private val NAMED_TOKENS = FUNCTION_NAMES.filter { it != "√" } + "e"
    private const val TRAILING_JUNK = "+−×÷^-*/."

    fun evaluate(expression: String, angle: AngleMode = AngleMode.DEG): EvalResult = try {
        val tokens = tokenize(balance(expression))
        EvalResult.Success(Parser(tokens, angle).parse())
    } catch (e: CalcException) {
        EvalResult.Failure(e.error)
    }

    /** Best-effort live value: trailing operators and dangling "fn(" are ignored. */
    fun preview(expression: String, angle: AngleMode = AngleMode.DEG): Double? {
        val trimmed = trimTrailing(expression)
        if (trimmed.isEmpty()) return null
        return (evaluate(trimmed, angle) as? EvalResult.Success)?.value
    }

    /** Appends any missing closing parentheses. */
    fun balance(expression: String): String {
        val open = expression.count { it == '(' } - expression.count { it == ')' }
        return if (open > 0) expression + ")".repeat(open) else expression
    }

    fun trimTrailing(expression: String): String {
        var s = expression
        while (true) {
            val before = s
            s = s.trimEnd { it in TRAILING_JUNK }
            if (s.endsWith("(")) {
                s = s.dropLast(1)
                FUNCTION_NAMES.firstOrNull { s.endsWith(it) }?.let { s = s.dropLast(it.length) }
            }
            if (s == before) return s
        }
    }

    /** Removes the last logical token: a whole "sin(" or a single character. */
    fun removeLastToken(expression: String): String {
        if (expression.isEmpty()) return expression
        for (name in FUNCTION_NAMES) {
            if (expression.endsWith("$name(")) return expression.dropLast(name.length + 1)
        }
        return expression.dropLast(1)
    }

    // ---- Tokenizer ----------------------------------------------------------------------------

    private fun tokenize(src: String): List<Token> {
        val out = ArrayList<Token>()
        var i = 0
        while (i < src.length) {
            val ch = src[i]
            when {
                ch.isWhitespace() -> i++
                ch in '0'..'9' || ch == '.' -> {
                    var j = i
                    var dots = 0
                    while (j < src.length && (src[j] in '0'..'9' || src[j] == '.')) {
                        if (src[j] == '.') dots++
                        j++
                    }
                    val text = src.substring(i, j)
                    if (dots > 1 || text == ".") fail()
                    out.add(Token.Num(text.toDouble()))
                    i = j
                }
                ch == '+' -> { out.add(Token.Op('+')); i++ }
                ch == '-' || ch == '−' || ch == '–' -> { out.add(Token.Op('-')); i++ }
                ch == '*' || ch == '×' -> { out.add(Token.Op('*')); i++ }
                ch == '/' || ch == '÷' -> { out.add(Token.Op('/')); i++ }
                ch == '^' -> { out.add(Token.Op('^')); i++ }
                ch == '(' -> { out.add(Token.LParen); i++ }
                ch == ')' -> { out.add(Token.RParen); i++ }
                ch == '%' -> { out.add(Token.Percent); i++ }
                ch == '!' -> { out.add(Token.Factorial); i++ }
                ch == 'π' -> { out.add(Token.Const(PI)); i++ }
                ch == '√' -> { out.add(Token.Func("√")); i++ }
                else -> {
                    val name = NAMED_TOKENS.firstOrNull { src.startsWith(it, i) } ?: fail()
                    out.add(if (name == "e") Token.Const(E) else Token.Func(name))
                    i += name.length
                }
            }
        }
        return out
    }

    // ---- Parser -------------------------------------------------------------------------------

    private class Num(val v: Double, val percent: Boolean = false)

    private class Parser(private val t: List<Token>, private val angle: AngleMode) {
        private var p = 0

        fun parse(): Double {
            if (t.isEmpty()) fail()
            val result = expr().v
            if (p != t.size) fail()
            if (result.isNaN()) fail(CalcError.UNDEFINED)
            if (result.isInfinite()) fail(CalcError.OVERFLOW)
            return result
        }

        private fun peek(): Token? = t.getOrNull(p)
        private fun peekOp(vararg symbols: Char): Boolean {
            val tok = peek()
            return tok is Token.Op && tok.symbol in symbols
        }

        private fun expr(): Num {
            var left = term()
            while (peekOp('+', '-')) {
                val plus = (t[p++] as Token.Op).symbol == '+'
                val right = term()
                val rv = if (right.percent && !left.percent) left.v * right.v else right.v
                left = Num(if (plus) left.v + rv else left.v - rv)
            }
            return left
        }

        private fun startsPrimary(): Boolean = when (peek()) {
            is Token.Num, Token.LParen, is Token.Func, is Token.Const -> true
            else -> false
        }

        private fun term(): Num {
            var left = unary()
            while (true) {
                if (peekOp('*', '/')) {
                    val mul = (t[p++] as Token.Op).symbol == '*'
                    val right = unary()
                    left = if (mul) {
                        Num(left.v * right.v)
                    } else {
                        if (right.v == 0.0) fail(CalcError.DIVIDE_BY_ZERO)
                        Num(left.v / right.v)
                    }
                } else if (startsPrimary()) {
                    left = Num(left.v * power().v) // implicit multiplication: 2π, 3(4+1)
                } else {
                    return left
                }
            }
        }

        private fun unary(): Num {
            if (peekOp('-')) { p++; return Num(-unary().v) }
            if (peekOp('+')) { p++; return unary() }
            return power()
        }

        private fun power(): Num {
            val base = postfix()
            if (peekOp('^')) {
                p++
                val exponent = unary()
                if (base.v == 0.0 && exponent.v < 0) fail(CalcError.DIVIDE_BY_ZERO)
                val r = base.v.pow(exponent.v)
                if (r.isNaN()) fail(CalcError.UNDEFINED)
                return Num(r)
            }
            return base
        }

        private fun postfix(): Num {
            var n = primary()
            while (true) {
                when (peek()) {
                    Token.Percent -> { p++; n = Num(n.v / 100.0, percent = true) }
                    Token.Factorial -> { p++; n = Num(factorial(n.v)) }
                    else -> return n
                }
            }
        }

        private fun primary(): Num {
            when (val tok = peek()) {
                is Token.Num -> { p++; return Num(tok.value) }
                is Token.Const -> { p++; return Num(tok.value) }
                Token.LParen -> {
                    p++
                    val inner = expr()
                    if (peek() != Token.RParen) fail()
                    p++
                    return Num(inner.v)
                }
                is Token.Func -> {
                    p++
                    if (peek() != Token.LParen) fail()
                    p++
                    val arg = expr()
                    if (peek() != Token.RParen) fail()
                    p++
                    return Num(apply(tok.name, arg.v))
                }
                else -> fail()
            }
        }

        private fun toRad(x: Double) = if (angle == AngleMode.DEG) Math.toRadians(x) else x
        private fun fromRad(x: Double) = if (angle == AngleMode.DEG) Math.toDegrees(x) else x
        private fun clean(v: Double) = if (abs(v) < 1e-12) 0.0 else v

        private fun apply(name: String, x: Double): Double = when (name) {
            "sin" -> clean(sin(toRad(x)))
            "cos" -> clean(cos(toRad(x)))
            "tan" -> {
                val c = cos(toRad(x))
                if (abs(c) < 1e-12) fail(CalcError.UNDEFINED)
                clean(sin(toRad(x)) / c)
            }
            "asin" -> if (x < -1 || x > 1) fail(CalcError.UNDEFINED) else clean(fromRad(asin(x)))
            "acos" -> if (x < -1 || x > 1) fail(CalcError.UNDEFINED) else clean(fromRad(acos(x)))
            "atan" -> clean(fromRad(atan(x)))
            "ln" -> if (x <= 0) fail(CalcError.UNDEFINED) else ln(x)
            "log" -> if (x <= 0) fail(CalcError.UNDEFINED) else log10(x)
            "√" -> if (x < 0) fail(CalcError.UNDEFINED) else sqrt(x)
            else -> fail()
        }

        private fun factorial(x: Double): Double {
            if (x < 0 || x != floor(x)) fail(CalcError.UNDEFINED)
            if (x > 170) fail(CalcError.OVERFLOW)
            var r = 1.0
            for (i in 2..x.toInt()) r *= i
            return r
        }
    }
}
