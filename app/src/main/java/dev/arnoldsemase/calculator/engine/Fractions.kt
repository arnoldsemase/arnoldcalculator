package dev.arnoldsemase.calculator.engine

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/** An exact fraction, always stored in lowest terms with a positive denominator. */
class Fraction private constructor(val num: BigInteger, val den: BigInteger) {

    val isInteger: Boolean get() = den == BigInteger.ONE
    val isNegative: Boolean get() = num.signum() < 0

    operator fun plus(o: Fraction): Fraction = of(num * o.den + o.num * den, den * o.den)!!
    operator fun minus(o: Fraction): Fraction = of(num * o.den - o.num * den, den * o.den)!!
    operator fun times(o: Fraction): Fraction = of(num * o.num, den * o.den)!!

    /** Null when dividing by zero. */
    operator fun div(o: Fraction): Fraction? = of(num * o.den, den * o.num)

    /** "19/12", or just "3" for a whole number. */
    fun improper(): String = if (isInteger) num.toString() else "$num/$den"

    /** Whole part (without sign) and remainder (without sign); the remainder is null for whole numbers. */
    fun mixedParts(): Pair<BigInteger, Fraction?> {
        val abs = num.abs()
        val whole = abs / den
        val rest = abs % den
        return whole to if (rest.signum() == 0) null else of(rest, den)
    }

    /** "1 7/12", "-3/4" or "5". */
    fun mixedText(): String {
        val (whole, rest) = mixedParts()
        val sign = if (isNegative) "-" else ""
        return when {
            rest == null -> "$sign$whole"
            whole.signum() == 0 -> "$sign${rest.improper()}"
            else -> "$sign$whole ${rest.improper()}"
        }
    }

    /** Decimal text rounded to [places], trailing zeros removed. */
    fun decimalPlain(places: Int = 4): String {
        val d = BigDecimal(num).divide(BigDecimal(den), places, RoundingMode.HALF_UP).stripTrailingZeros()
        return if (d.signum() == 0) "0" else d.toPlainString()
    }

    /** True when the decimal expansion ends (the denominator has only the prime factors 2 and 5). */
    val isTerminating: Boolean
        get() {
            var d = den
            val two = BigInteger.valueOf(2)
            val five = BigInteger.valueOf(5)
            while (d.mod(two).signum() == 0) d /= two
            while (d.mod(five).signum() == 0) d /= five
            return d == BigInteger.ONE
        }

    override fun equals(other: Any?): Boolean = other is Fraction && num == other.num && den == other.den
    override fun hashCode(): Int = 31 * num.hashCode() + den.hashCode()
    override fun toString(): String = improper()

    companion object {
        /** Null when the denominator is zero. */
        fun of(num: BigInteger, den: BigInteger): Fraction? {
            if (den.signum() == 0) return null
            val g = num.gcd(den)
            var n = num / g
            var d = den / g
            if (d.signum() < 0) {
                n = n.negate()
                d = d.negate()
            }
            return Fraction(n, d)
        }

        fun ofInt(value: BigInteger): Fraction = Fraction(value, BigInteger.ONE)
    }
}

enum class FractionOp(val symbol: String, val spoken: String) {
    PLUS("+", "add"),
    MINUS("−", "subtract"),
    TIMES("×", "multiply"),
    DIVIDE("÷", "divide"),
}

/** What the user has typed for one fraction: optional whole part, numerator and denominator as digits. */
data class FractionInput(
    val negative: Boolean = false,
    /** True when the whole-number box is shown. */
    val mixed: Boolean = false,
    val whole: String = "",
    val num: String = "",
    val den: String = "",
) {
    sealed interface Value {
        data class Ok(val fraction: Fraction) : Value
        data object Incomplete : Value
        data object ZeroDenominator : Value
    }

    fun value(): Value {
        val w = whole.toBigIntegerOrNull()
        val n = num.toBigIntegerOrNull()
        val d = den.toBigIntegerOrNull()
        val sign = if (negative) BigInteger.ONE.negate() else BigInteger.ONE
        return when {
            n == null && d == null -> if (w == null) Value.Incomplete else Value.Ok(Fraction.ofInt(w * sign))
            n == null || d == null -> Value.Incomplete
            d.signum() == 0 -> Value.ZeroDenominator
            else -> Value.Ok(Fraction.of(((w ?: BigInteger.ZERO) * d + n) * sign, d)!!)
        }
    }
}

sealed interface FractionOutcome {
    /**
     * @property step the working, e.g. "LCD 12 → 9/12 + 10/12"
     * @property unreduced the answer before it was simplified, or null if it was already in lowest terms
     */
    data class Answer(val value: Fraction, val step: String, val unreduced: String?) : FractionOutcome

    /** Something is still missing, so there is nothing to show yet. */
    data object Waiting : FractionOutcome

    data class Problem(val message: String) : FractionOutcome
}

object FractionCalc {

    fun compute(a: FractionInput, op: FractionOp, b: FractionInput): FractionOutcome {
        val av = a.value()
        val bv = b.value()
        if (av is FractionInput.Value.ZeroDenominator || bv is FractionInput.Value.ZeroDenominator) {
            return FractionOutcome.Problem("A denominator can't be zero")
        }
        if (av !is FractionInput.Value.Ok || bv !is FractionInput.Value.Ok) return FractionOutcome.Waiting
        return compute(av.fraction, op, bv.fraction)
    }

    fun compute(x: Fraction, op: FractionOp, y: Fraction): FractionOutcome {
        val value: Fraction
        val step: String
        val rawNum: BigInteger
        val rawDen: BigInteger
        when (op) {
            FractionOp.PLUS, FractionOp.MINUS -> {
                val lcd = x.den / x.den.gcd(y.den) * y.den
                val tx = x.num * (lcd / x.den)
                val ty = y.num * (lcd / y.den)
                val sym = if (op == FractionOp.PLUS) "+" else "−"
                value = if (op == FractionOp.PLUS) x + y else x - y
                step = "LCD $lcd → $tx/$lcd $sym $ty/$lcd"
                rawNum = if (op == FractionOp.PLUS) tx + ty else tx - ty
                rawDen = lcd
            }
            FractionOp.TIMES -> {
                value = x * y
                step = "${x.num}×${y.num} / ${x.den}×${y.den}"
                rawNum = x.num * y.num
                rawDen = x.den * y.den
            }
            FractionOp.DIVIDE -> {
                val flipped = Fraction.of(y.den, y.num) ?: return FractionOutcome.Problem("Can't divide by zero")
                value = x * flipped
                step = "${x.improper()} × ${flipped.improper()} (flip the second)"
                rawNum = x.num * flipped.num
                rawDen = x.den * flipped.den
            }
        }
        // Compare in lowest terms so a sign difference ("−3/4" vs "3/−4") never counts as unreduced.
        val raw = if (rawDen.signum() < 0) "${rawNum.negate()}/${rawDen.negate()}" else "$rawNum/$rawDen"
        val unreduced = raw.takeIf { it != value.improper() && !(value.isInteger && rawDen == BigInteger.ONE) }
        return FractionOutcome.Answer(value, step, unreduced)
    }
}
