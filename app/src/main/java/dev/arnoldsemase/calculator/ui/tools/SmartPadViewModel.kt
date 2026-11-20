package dev.arnoldsemase.calculator.ui.tools

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.arnoldsemase.calculator.data.RatesRepository
import kotlinx.coroutines.launch

/**
 * Holds the Smart Pad text (so it survives leaving the screen) and the cached exchange rates.
 * Rates are read from the local cache only: Smart Pad never touches the network.
 */
class SmartPadViewModel(private val ratesRepository: RatesRepository) : ViewModel() {

    var text by mutableStateOf("")
        private set

    var rates: Map<String, Double>? by mutableStateOf(null)
        private set

    fun onTextChange(value: String) {
        text = value
    }

    /** Re-reads the cache, so rates refreshed in the Currency tool are picked up. */
    fun loadRates() {
        viewModelScope.launch { rates = ratesRepository.cached()?.rates }
    }
}
