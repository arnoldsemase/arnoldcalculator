package dev.arnoldsemase.calculator.ui.tools

import androidx.lifecycle.ViewModel
import dev.arnoldsemase.calculator.engine.AgeResult
import dev.arnoldsemase.calculator.engine.DateCalc
import dev.arnoldsemase.calculator.engine.DateDifference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

enum class DateMode(val label: String) {
    DIFFERENCE("Difference"),
    ADD("Add / subtract"),
    AGE("Age"),
}

enum class AmountUnit { YEARS, MONTHS, WEEKS, DAYS }

data class DatesUiState(
    val today: LocalDate,
    val mode: DateMode = DateMode.DIFFERENCE,
    // Difference
    val from: LocalDate = today,
    val to: LocalDate = LocalDate.of(today.year, 12, 31),
    val includeEnd: Boolean = false,
    // Add / subtract
    val start: LocalDate = today,
    val subtract: Boolean = false,
    val years: String = "",
    val months: String = "",
    val weeks: String = "",
    val days: String = "30",
    // Age
    val birth: LocalDate? = null,
    val asOf: LocalDate = today,
    /** Which of the two dates in the current mode the calendar is editing. */
    val secondActive: Boolean = false,
    val viewMonth: YearMonth = YearMonth.from(today),
) {
    /** The date the calendar is currently editing (null for an empty birth date). */
    val activeDate: LocalDate?
        get() = when (mode) {
            DateMode.DIFFERENCE -> if (secondActive) to else from
            DateMode.ADD -> start
            DateMode.AGE -> if (secondActive) asOf else birth
        }

    /** Whether this mode has two dates to choose between. */
    val hasTwoDates: Boolean get() = mode != DateMode.ADD

    val difference: DateDifference get() = DateCalc.difference(from, to, includeEnd)

    val addedDate: LocalDate?
        get() = DateCalc.add(
            start = start,
            years = years.toLongOrNull() ?: 0,
            months = months.toLongOrNull() ?: 0,
            days = (weeks.toLongOrNull() ?: 0) * 7 + (days.toLongOrNull() ?: 0),
            subtract = subtract,
        )

    /** Null while no birth date is picked, or when it is after the as-of date. */
    val age: AgeResult? get() = birth?.let { DateCalc.age(it, asOf) }
}

class DatesViewModel(today: LocalDate = LocalDate.now()) : ViewModel() {

    private val _state = MutableStateFlow(DatesUiState(today = today))
    val state: StateFlow<DatesUiState> = _state.asStateFlow()

    fun setMode(mode: DateMode) = _state.update {
        val moved = it.copy(mode = mode, secondActive = false)
        moved.copy(viewMonth = YearMonth.from(moved.activeDate ?: it.today))
    }

    /** Chooses which date the calendar edits (Difference and Age have two). */
    fun selectField(second: Boolean) = _state.update {
        val moved = it.copy(secondActive = second)
        moved.copy(viewMonth = YearMonth.from(moved.activeDate ?: it.today))
    }

    fun pick(date: LocalDate) = _state.update { s ->
        val picked = when (s.mode) {
            DateMode.DIFFERENCE -> if (s.secondActive) s.copy(to = date) else s.copy(from = date, secondActive = true)
            DateMode.ADD -> s.copy(start = date)
            DateMode.AGE -> if (s.secondActive) s.copy(asOf = date) else s.copy(birth = date)
        }
        picked.copy(viewMonth = YearMonth.from(date))
    }

    fun pickToday() = pick(_state.value.today)

    fun shiftMonths(by: Long) = _state.update { it.copy(viewMonth = it.viewMonth.plusMonths(by)) }

    fun toggleIncludeEnd() = _state.update { it.copy(includeEnd = !it.includeEnd) }

    fun setSubtract(subtract: Boolean) = _state.update { it.copy(subtract = subtract) }

    fun setAmount(unit: AmountUnit, text: String) {
        val clean = text.filter { it in '0'..'9' }.take(5)
        _state.update {
            when (unit) {
                AmountUnit.YEARS -> it.copy(years = clean)
                AmountUnit.MONTHS -> it.copy(months = clean)
                AmountUnit.WEEKS -> it.copy(weeks = clean)
                AmountUnit.DAYS -> it.copy(days = clean)
            }
        }
    }
}
