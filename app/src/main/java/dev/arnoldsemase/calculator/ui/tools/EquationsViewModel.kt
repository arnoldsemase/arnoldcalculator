package dev.arnoldsemase.calculator.ui.tools

import androidx.lifecycle.ViewModel
import dev.arnoldsemase.calculator.engine.EquationOutcome
import dev.arnoldsemase.calculator.engine.EquationSolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class EquationMode(val label: String, val hint: String) {
    LINEAR("Linear", "Try 2x + 3 = 11"),
    QUADRATIC("Quadratic", "Try x² − 5x + 6 = 0"),
    SYSTEM("System 2×2", "Try 2x + y = 5 and x − y = 1"),
}

private const val MAX_LENGTH = 60

data class EquationsUiState(
    val mode: EquationMode = EquationMode.QUADRATIC,
    val linear: String = "",
    val quadratic: String = "",
    val first: String = "",
    val second: String = "",
    /** In System mode: whether the second equation is the one being typed. */
    val secondActive: Boolean = false,
) {
    /** The text of the equation being typed. */
    val activeText: String
        get() = when (mode) {
            EquationMode.LINEAR -> linear
            EquationMode.QUADRATIC -> quadratic
            EquationMode.SYSTEM -> if (secondActive) second else first
        }

    fun withActiveText(text: String): EquationsUiState = when (mode) {
        EquationMode.LINEAR -> copy(linear = text)
        EquationMode.QUADRATIC -> copy(quadratic = text)
        EquationMode.SYSTEM -> if (secondActive) copy(second = text) else copy(first = text)
    }

    val outcome: EquationOutcome
        get() = when (mode) {
            EquationMode.LINEAR -> EquationSolver.linear(linear)
            EquationMode.QUADRATIC -> EquationSolver.quadratic(quadratic)
            EquationMode.SYSTEM -> EquationSolver.system(first, second)
        }
}

class EquationsViewModel : ViewModel() {

    private val _state = MutableStateFlow(EquationsUiState())
    val state: StateFlow<EquationsUiState> = _state.asStateFlow()

    fun setMode(mode: EquationMode) = _state.update { it.copy(mode = mode, secondActive = false) }

    fun selectField(second: Boolean) = _state.update { it.copy(secondActive = second) }

    fun type(text: String) = _state.update {
        if (it.activeText.length + text.length > MAX_LENGTH) it else it.withActiveText(it.activeText + text)
    }

    fun backspace() = _state.update { it.withActiveText(it.activeText.dropLast(1)) }

    /** Clears the equation being typed (both of them in System mode). */
    fun clear() = _state.update {
        if (it.mode == EquationMode.SYSTEM) it.copy(first = "", second = "", secondActive = false)
        else it.withActiveText("")
    }
}
