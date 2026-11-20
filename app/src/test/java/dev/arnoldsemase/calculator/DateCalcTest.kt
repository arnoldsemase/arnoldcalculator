package dev.arnoldsemase.calculator

import dev.arnoldsemase.calculator.engine.DateCalc
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateCalcTest {
    private val nov19 = LocalDate.of(2026, 11, 19)
    private val dec31 = LocalDate.of(2026, 12, 31)

    @Test fun differenceBreakdown() {
        val d = DateCalc.difference(nov19, dec31, includeEnd = false)
        assertEquals(42, d.days)
        assertEquals(6, d.weeks)
        assertEquals(0, d.extraDays)
        assertEquals(1, d.months)
        assertEquals(12, d.dayPart)
        assertEquals(30, d.workingDays)
        assertFalse(d.backwards)
    }

    @Test fun includingTheEndDateAddsOneDay() {
        val d = DateCalc.difference(nov19, dec31, includeEnd = true)
        assertEquals(43, d.days)
        assertEquals(31, d.workingDays)
        assertEquals(13, d.dayPart)
    }

    @Test fun sameDay() {
        assertEquals(0, DateCalc.difference(nov19, nov19, false).days)
        assertEquals(1, DateCalc.difference(nov19, nov19, true).days)
    }

    @Test fun backwardsGivesTheSameSizeAndFlagsIt() {
        val d = DateCalc.difference(dec31, nov19, includeEnd = false)
        assertEquals(42, d.days)
        assertTrue(d.backwards)
        assertEquals(30, d.workingDays)
    }

    @Test fun leapYearIsCounted() {
        assertEquals(366, DateCalc.difference(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1), false).days)
        assertEquals(365, DateCalc.difference(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1), false).days)
    }

    @Test fun weekdaysSkipWeekends() {
        // Mon 2026-11-16 to Mon 2026-11-23 (exclusive): Mon–Fri only.
        assertEquals(5, DateCalc.weekdays(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 23)))
        // Sat to Mon (exclusive of Mon): Sat + Sun only.
        assertEquals(0, DateCalc.weekdays(LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 23)))
        assertEquals(0, DateCalc.weekdays(nov19, nov19))
    }

    @Test fun addAndSubtract() {
        assertEquals(LocalDate.of(2027, 2, 17), DateCalc.add(nov19, 0, 0, 90, subtract = false))
        assertEquals(LocalDate.of(2024, 2, 29), DateCalc.add(LocalDate.of(2024, 1, 31), 0, 1, 0, false))
        assertEquals(LocalDate.of(2023, 2, 28), DateCalc.add(LocalDate.of(2024, 2, 29), 1, 0, 0, subtract = true))
        assertEquals(nov19, DateCalc.add(nov19, 0, 0, 0, false))
    }

    @Test fun addingAbsurdAmountsDoesNotCrash() {
        assertNull(DateCalc.add(nov19, 5_000_000_000L, 0, 0, false))
    }

    @Test fun age() {
        val a = DateCalc.age(LocalDate.of(1990, 5, 15), nov19)!!
        assertEquals(36, a.years)
        assertEquals(6, a.months)
        assertEquals(4, a.days)
        assertEquals(13337, a.totalDays)
        assertEquals(LocalDate.of(2027, 5, 15), a.nextBirthday)
        assertEquals(177, a.daysToNextBirthday)
    }

    @Test fun birthdayToday() {
        val a = DateCalc.age(LocalDate.of(2000, 11, 19), nov19)!!
        assertEquals(26, a.years)
        assertEquals(0, a.daysToNextBirthday)
    }

    @Test fun leapDayBirthdayAndFutureBirth() {
        val a = DateCalc.age(LocalDate.of(2000, 2, 29), nov19)!!
        assertEquals(LocalDate.of(2027, 2, 28), a.nextBirthday)
        assertNull(DateCalc.age(LocalDate.of(2030, 1, 1), nov19))
    }
}
