package dev.arnoldsemase.calculator.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.arnoldsemase.calculator.data.AppSettings
import dev.arnoldsemase.calculator.data.SettingsRepository
import dev.arnoldsemase.calculator.data.ThemeMode
import dev.arnoldsemase.calculator.engine.DecimalMark
import dev.arnoldsemase.calculator.engine.GroupingStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repository.setTheme(mode) }
    fun setHaptics(enabled: Boolean) = viewModelScope.launch { repository.setHaptics(enabled) }
    fun setGrouping(style: GroupingStyle) = viewModelScope.launch { repository.setGrouping(style) }
    fun setDecimalMark(mark: DecimalMark) = viewModelScope.launch { repository.setDecimalMark(mark) }
    fun setMaxDecimals(places: Int) = viewModelScope.launch { repository.setMaxDecimals(places) }
}
