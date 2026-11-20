package dev.arnoldsemase.calculator.engine

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The five percentage questions. In every mode the user fills two numbers, [A] then [B]:
 *
 * | Mode     | A            | B     | Main result              | Extra lines                 |
 * |----------|--------------|-------|--------------------------|-----------------------------|
 * | OF       | percent      | base  | A% of B                  | B plus A%, B minus A%       |
 * | CHANGE   | from         | to    | % change from A to B     | difference                  |
 * | IS       | part         | whole | A as a % of B            | A ÷ B as a decimal          |
 * | TIP      | tip percent  | bill  | tip amount               | total to pay                |
 * | DISCOUNT | discount %   | price | price after the discount | amount saved                |
 */
enum class PercentMode(val label: String) {
    OF("X% of Y"),
    CHANGE("% change"),
    IS("X is ?% of Y"),
    TIP("Tip"),
    DISCOUNT("Discount"),
}

data class PercentLine(val label: String, val value: Double, val isPercent: Boolean = false)

data class PercentResult(
    val title: String,
    val value: Double,
    val isPercent: Boolean,
    val lines: List<PercentLine>,
)

object PercentageCalc {

    /** Returns null while an input is missing or the question has no answer (such as dividing by zero). */
    fun compute(mode: PercentMode, a: Double?, b: Double?): PercentResult? {
        if (a == null || b == null) return null
        val r = when (mode) {
            PercentMode.OF -> PercentResult(
                "Result", a * b / 100.0, false,
                listOf(
                    PercentLine("Base plus %", b * (1.0 + a / 100.0)),
                    PercentLine("Base minus %", b * (1.0 - a / 100.0)),
                ),
            )
            PercentMode.CHANGE -> {
                if (a == 0.0) return null
                PercentResult(
                    "Change", (b - a) / Math.abs(a) * 100.0, true,
                    listOf(PercentLine("Difference", b - a)),
                )
            }
            PercentMode.IS -> {
                if (b == 0.0) return null
                PercentResult(
                    "Percentage", a / b * 100.0, true,
                    listOf(PercentLine("As a decimal", a / b)),
                )
            }
            PercentMode.TIP -> PercentResult(
                "Tip", b * a / 100.0, false,
                listOf(PercentLine("Total", b + b * a / 100.0)),
            )
            PercentMode.DISCOUNT -> PercentResult(
                "You pay", b * (1.0 - a / 100.0), false,
                listOf(PercentLine("You save", b * a / 100.0)),
            )
        }
        return if (r.value.isFinite()) r else null
    }

    /** Up to four decimals, trailing zeros removed, as plain text. */
    fun plain(value: Double): String {
        if (!value.isFinite()) return "0"
        val bd = BigDecimal(value).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros()
        return if (bd.signum() == 0) "0" else bd.toPlainString()
    }
}
