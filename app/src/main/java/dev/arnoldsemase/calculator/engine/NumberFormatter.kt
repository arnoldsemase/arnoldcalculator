package dev.arnoldsemase.calculator.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class GroupingStyle(val label: String) {
    COMMA("1,234"),
    SPACE("1 234"),
    NONE("1234"),
}

enum class DecimalMark(val label: String, val char: Char) {
    POINT("1.5", '.'),
    COMMA("1,5", ','),
}

data class NumberFormatSettings(
    val grouping: GroupingStyle = GroupingStyle.COMMA,
    val decimalMark: DecimalMark = DecimalMark.POINT,
    /** [AUTO] shows as many decimals as the result needs (up to 12 significant digits). */
    val maxDecimals: Int = AUTO,
) {
    companion object {
        const val AUTO = -1
    }
}

/**
 * Display-only formatting. Calculations and history always keep the "plain" form
 * (period decimal, no grouping), so changing preferences never corrupts stored data.
 */
class NumberFormatter(private val settings: NumberFormatSettings = NumberFormatSettings()) {

    private val groupSeparator: String
        get() = when (settings.grouping) {
            GroupingStyle.NONE -> ""
            GroupingStyle.SPACE -> "\u2009"
            GroupingStyle.COMMA -> if (settings.decimalMark == DecimalMark.COMMA) "." else ","
        }

    /** Formats a plain result string such as "1234567.891". */
    fun display(plain: String): String {
        var v = plain.toBigDecimalOrNull() ?: return plain
        if (settings.maxDecimals >= 0 && v.scale() > settings.maxDecimals) {
            v = v.setScale(settings.maxDecimals, RoundingMode.HALF_UP)
        }
        v = v.stripTrailingZeros()
        if (v.signum() == 0) return "0"
        val abs = v.abs()
        if (abs >= SCI_HIGH || abs < SCI_LOW) return scientific(v)
        return group(v.toPlainString())
    }

    /** Formats a number exactly as typed, keeping a trailing "." or trailing zeros. */
    fun typing(raw: String): String = group(raw)

    /** Pretty-prints an expression: grouped numbers and breathing room around binary operators. */
    fun expression(raw: String): String {
        val sb = StringBuilder()
        var prev: Char? = null
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c in '0'..'9' || c == '.') {
                var j = i
                while (j < raw.length && (raw[j] in '0'..'9' || raw[j] == '.')) j++
                sb.append(group(raw.substring(i, j)))
                prev = raw[j - 1]
                i = j
                continue
            }
            when (c) {
                '+', '×', '÷' -> sb.append(THIN).append(c).append(THIN)
                '-', '−' -> {
                    val binary = prev != null && (prev in '0'..'9' || prev in ")%!πe.")
                    if (binary) sb.append(THIN).append('−').append(THIN) else sb.append('−')
                }
                else -> sb.append(c)
            }
            prev = c
            i++
        }
        return sb.toString().trim()
    }

    private fun group(raw: String): String {
        val negative = raw.startsWith("-") || raw.startsWith("−")
        val body = raw.trimStart('-', '−')
        val parts = body.split('.')
        val intPart = parts[0]
        val sep = groupSeparator
        val grouped = if (sep.isEmpty() || intPart.length <= 3) {
            intPart
        } else {
            intPart.reversed().chunked(3).joinToString(sep).reversed()
        }
        val sb = StringBuilder()
        if (negative) sb.append('−')
        sb.append(grouped)
        if (parts.size > 1) sb.append(settings.decimalMark.char).append(parts[1])
        return sb.toString()
    }

    private fun scientific(v: BigDecimal): String {
        val r = v.round(MathContext(10, RoundingMode.HALF_UP)).stripTrailingZeros()
        val exp = r.precision() - r.scale() - 1
        val mantissa = r.movePointLeft(exp).stripTrailingZeros().toPlainString()
        return (mantissa.replace('.', settings.decimalMark.char) + "E" + exp).replace('-', '−')
    }

    companion object {
        private const val THIN = '\u2009'
        private val SCI_HIGH = BigDecimal("1E15")
        private val SCI_LOW = BigDecimal("1E-9")

        /** Canonical text for a computed value: 12 significant digits, no float noise (0.1+0.2 → "0.3"). */
        fun toPlain(value: Double): String {
            val bd = BigDecimal(value).round(MathContext(12, RoundingMode.HALF_UP)).stripTrailingZeros()
            return if (bd.signum() == 0) "0" else bd.toPlainString()
        }
    }
}
