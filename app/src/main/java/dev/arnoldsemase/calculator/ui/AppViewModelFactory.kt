package dev.arnoldsemase.calculator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.arnoldsemase.calculator.AppContainer
import dev.arnoldsemase.calculator.ui.calculator.CalculatorViewModel
import dev.arnoldsemase.calculator.ui.history.HistoryViewModel
import dev.arnoldsemase.calculator.ui.settings.SettingsViewModel
import dev.arnoldsemase.calculator.ui.tools.CurrencyViewModel
import dev.arnoldsemase.calculator.ui.tools.DatesViewModel
import dev.arnoldsemase.calculator.ui.tools.EquationsViewModel
import dev.arnoldsemase.calculator.ui.tools.FractionsViewModel
import dev.arnoldsemase.calculator.ui.tools.SmartPadViewModel

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(CalculatorViewModel::class.java) ->
            CalculatorViewModel(container.historyRepository, container.settingsRepository) as T
        modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
            HistoryViewModel(container.historyRepository, container.settingsRepository) as T
        modelClass.isAssignableFrom(CurrencyViewModel::class.java) ->
            CurrencyViewModel(container.ratesRepository) as T
        modelClass.isAssignableFrom(FractionsViewModel::class.java) ->
            FractionsViewModel() as T
        modelClass.isAssignableFrom(DatesViewModel::class.java) ->
            DatesViewModel() as T
        modelClass.isAssignableFrom(EquationsViewModel::class.java) ->
            EquationsViewModel() as T
        modelClass.isAssignableFrom(SmartPadViewModel::class.java) ->
            SmartPadViewModel(container.ratesRepository) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(container.settingsRepository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
