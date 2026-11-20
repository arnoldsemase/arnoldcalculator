package dev.arnoldsemase.calculator.engine

import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

data class DateDifference(
    /** Whole days between the two dates (plus one if the end date counts). Never negative. */
    val days: Long,
    val years: Int,
    val months: Int,
    /** The day part of the years / months / days breakdown. */
    val dayPart: Int,
    /** Monday to Friday only. */
    val workingDays: Long,
    /** True when the second date is earlier than the first. */
    val backwards: Boolean,
) {
    val weeks: Long get() = days / 7
    val extraDays: Long get() = days % 7
}

data class AgeResult(
    val years: Int,
    val months: Int,
    val days: Int,
    val totalDays: Long,
    val nextBirthday: LocalDate,
    val daysToNextBirthday: Long,
)

object DateCalc {

    fun difference(from: LocalDate, to: LocalDate, includeEnd: Boolean): DateDifference {
        val backwards = to.isBefore(from)
        val start = if (backwards) to else from
        val end = if (backwards) from else to
        val endExclusive = if (includeEnd) end.plusDays(1) else end
        val p = Period.between(start, endExclusive)
        return DateDifference(
            days = ChronoUnit.DAYS.between(start, endExclusive),
            years = p.years,
            months = p.months,
            dayPart = p.days,
            workingDays = weekdays(start, endExclusive),
            backwards = backwards,
        )
    }

    /** Weekdays (Mon–Fri) in [start, endExclusive). */
    fun weekdays(start: LocalDate, endExclusive: LocalDate): Long {
        val n = ChronoUnit.DAYS.between(start, endExclusive)
        if (n <= 0) return 0
        var count = n / 7 * 5
        val first = start.plusDays(n / 7 * 7)
        for (i in 0 until (n % 7).toInt()) {
            val d = first.plusDays(i.toLong()).dayOfWeek
            if (d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY) count++
        }
        return count
    }

    /** Null if the result falls outside the dates Java can represent. */
    fun add(start: LocalDate, years: Long, months: Long, days: Long, subtract: Boolean): LocalDate? {
        val s = if (subtract) -1L else 1L
        return try {
            start.plusYears(s * years).plusMonths(s * months).plusDays(s * days)
        } catch (e: DateTimeException) {
            null
        } catch (e: ArithmeticException) {
            null
        }
    }

    /** Null if [asOf] is before [birth]. */
    fun age(birth: LocalDate, asOf: LocalDate): AgeResult? {
        if (asOf.isBefore(birth)) return null
        val p = Period.between(birth, asOf)
        var next = birth.withYear(asOf.year)
        if (next.isBefore(asOf)) next = birth.withYear(asOf.year + 1)
        return AgeResult(
            years = p.years,
            months = p.months,
            days = p.days,
            totalDays = ChronoUnit.DAYS.between(birth, asOf),
            nextBirthday = next,
            daysToNextBirthday = ChronoUnit.DAYS.between(asOf, next),
        )
    }
}
