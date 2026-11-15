package dev.arnoldsemase.calculator.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.arnoldsemase.calculator.data.HistoryEntry
import dev.arnoldsemase.calculator.data.HistoryRepository
import dev.arnoldsemase.calculator.data.SettingsRepository
import dev.arnoldsemase.calculator.engine.NumberFormatter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class HistoryItem(
    val entry: HistoryEntry,
    val expressionText: String,
    val resultText: String,
    val timeText: String,
)

data class HistoryGroup(val title: String, val items: List<HistoryItem>)

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Loaded(val groups: List<HistoryGroup>) : HistoryUiState
}

class HistoryViewModel(
    private val repository: HistoryRepository,
    settings: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<HistoryUiState> = combine(repository.entries, settings.settings) { entries, s ->
        HistoryUiState.Loaded(group(entries, NumberFormatter(s.numberFormat)))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)

    fun delete(entry: HistoryEntry) {
        viewModelScope.launch { repository.delete(entry.id) }
    }

    fun undoDelete(entry: HistoryEntry) {
        viewModelScope.launch { repository.restore(entry) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clear() }
    }

    private fun group(entries: List<HistoryEntry>, f: NumberFormatter): List<HistoryGroup> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val timeFmt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        val dateFmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        return entries
            .groupBy { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }
            .map { (date, list) ->
                HistoryGroup(
                    title = when (date) {
                        today -> "Today"
                        today.minusDays(1) -> "Yesterday"
                        else -> date.format(dateFmt)
                    },
                    items = list.map { e ->
                        HistoryItem(
                            entry = e,
                            expressionText = f.expression(e.expression),
                            resultText = f.display(e.result),
                            timeText = Instant.ofEpochMilli(e.createdAt).atZone(zone).format(timeFmt),
                        )
                    },
                )
            }
    }
}
