package dev.arnoldsemase.calculator.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.arnoldsemase.calculator.data.RatesRepository
import dev.arnoldsemase.calculator.engine.CurrencyConverter
import dev.arnoldsemase.calculator.engine.RateTable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CurrencyUiState(
    val table: RateTable? = null,
    val refreshing: Boolean = false,
    val failed: Boolean = false,
    val selected: List<String> = emptyList(),
    val active: String = "USD",
    /** What the user typed into the active row, as plain text. */
    val input: String = "1",
) {
    val amount: Double get() = input.toDoubleOrNull() ?: 0.0

    /** Value of [code] for the current input, or null while there are no rates. */
    fun valueOf(code: String): Double? =
        if (code == active) amount else table?.let { CurrencyConverter.convert(amount, active, code, it.rates) }

    fun rateFromActive(code: String): Double? =
        table?.let { CurrencyConverter.convert(1.0, active, code, it.rates) }
}

class CurrencyViewModel(private val repository: RatesRepository) : ViewModel(), NumberInput {

    private val _state = MutableStateFlow(
        repository.selection().let { (list, active) -> CurrencyUiState(selected = list, active = active) },
    )
    val state: StateFlow<CurrencyUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val cached = repository.cached()
            _state.update { it.copy(table = cached) }
            val stale = cached == null || System.currentTimeMillis() - cached.fetchedAtMillis > STALE_AFTER_MS
            if (stale) refresh()
        }
    }

    fun refresh() {
        if (_state.value.refreshing) return
        _state.update { it.copy(refreshing = true, failed = false) }
        viewModelScope.launch {
            repository.refresh().fold(
                onSuccess = { table -> _state.update { it.copy(table = table, refreshing = false, failed = false) } },
                onFailure = { _state.update { it.copy(refreshing = false, failed = true) } },
            )
        }
    }

    /** Makes [code] the row being typed into, carrying its current value across. */
    fun select(code: String) {
        val s = _state.value
        if (code == s.active) return
        val carried = s.valueOf(code)?.let { CurrencyConverter.moneyPlain(it) } ?: s.input
        _state.update { it.copy(active = code, input = carried) }
        save()
    }

    fun add(code: String) {
        _state.update { if (code in it.selected) it else it.copy(selected = it.selected + code) }
        save()
    }

    /** Removes a row, keeping at least two. The active row cannot be removed. */
    fun remove(code: String): Boolean {
        val s = _state.value
        if (s.selected.size <= 2 || code == s.active || code !in s.selected) return false
        _state.update { it.copy(selected = it.selected - code) }
        save()
        return true
    }

    override fun digits(d: String) = _state.update { it.copy(input = typeDigits(it.input, d)) }
    override fun decimal() = _state.update { it.copy(input = typeDecimal(it.input)) }
    override fun backspace() = _state.update { it.copy(input = typeBackspace(it.input)) }
    override fun clear() = _state.update { it.copy(input = "") }
    override fun negate() = _state.update { it.copy(input = typeNegate(it.input)) }

    private fun save() = _state.value.let { repository.saveSelection(it.selected, it.active) }

    private companion object {
        const val STALE_AFTER_MS = 12L * 60 * 60 * 1000
    }
}

/** Keypad actions shared by tools that type a single number. */
interface NumberInput {
    fun digits(d: String)
    fun decimal()
    fun backspace()
    fun clear()
    fun negate()
}
