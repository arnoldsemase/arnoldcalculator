package dev.arnoldsemase.calculator.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.arnoldsemase.calculator.data.AppSettings
import dev.arnoldsemase.calculator.data.HistoryRepository
import dev.arnoldsemase.calculator.data.SettingsRepository
import dev.arnoldsemase.calculator.engine.AngleMode
import dev.arnoldsemase.calculator.engine.CalcError
import dev.arnoldsemase.calculator.engine.EvalResult
import dev.arnoldsemase.calculator.engine.ExpressionEvaluator
import dev.arnoldsemase.calculator.engine.NumberFormatSettings
import dev.arnoldsemase.calculator.engine.NumberFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the keypad can do. Implemented by the ViewModel; keeps UI free of logic. */
interface CalculatorInput {
    fun digit(d: Char)
    fun decimal()
    fun operation(op: Char)
    fun function(name: String)
    fun constant(symbol: String)
    fun parenthesis()
    fun percent()
    fun factorial()
    fun suffix(text: String)
    fun backspace()
    fun clear()
    fun evaluate()
    fun toggleAngle()
    fun toggleScientific()
    fun memoryClear()
    fun memoryRecall()
    fun memoryAdd()
    fun memorySubtract()
    fun memoryStore()
}

data class CalcState(
    val expression: String = "",
    val result: String? = null,
    val committed: String? = null,
    val error: CalcError? = null,
    val errorTick: Int = 0,
    val scientific: Boolean = false,
    val angle: AngleMode = AngleMode.DEG,
    /** Value held in memory (MS / M+ / M−), or null when memory is empty. */
    val memory: Double? = null,
)

data class DisplayState(
    val primary: String = "0",
    val secondary: String = "",
    val isError: Boolean = false,
    val errorTick: Int = 0,
    val copyValue: String? = null,
    val scientific: Boolean = false,
    val angle: AngleMode = AngleMode.DEG,
    val decimalMark: Char = '.',
    /** Formatted memory value for the display chip, or null when memory is empty. */
    val memory: String? = null,
)

private val LONE_NUMBER = Regex("^−?[0-9]*\\.?[0-9]*$")
private const val OPERATORS = "+−×÷^"
private val OPERATOR_CHARS = charArrayOf('+', '−', '×', '÷', '^')
private const val MAX_DIGITS = 15

private fun endsWithOperand(s: String): Boolean {
    val last = s.lastOrNull() ?: return false
    return last in '0'..'9' || last in ")%!πe"
}

private fun trailingNumber(s: String): String = s.takeLastWhile { it in '0'..'9' || it == '.' }

class CalculatorViewModel(
    private val history: HistoryRepository,
    settings: SettingsRepository,
) : ViewModel(), CalculatorInput {

    private val state = MutableStateFlow(CalcState())
    private val appSettings: StateFlow<AppSettings> =
        settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val display: StateFlow<DisplayState> = combine(state, appSettings) { s, a ->
        buildDisplay(s, a.numberFormat)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildDisplay(CalcState(), NumberFormatSettings()))

    val hapticsEnabled: StateFlow<Boolean> =
        appSettings.map { it.haptics }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // ---- Input ----------------------------------------------------------------------------------

    private fun edit(continueFromResult: Boolean, transform: (String) -> String) {
        state.update { s ->
            val base = when {
                s.result != null -> if (continueFromResult) s.result.replace('-', '−') else ""
                else -> s.expression
            }
            s.copy(expression = transform(base), result = null, committed = null, error = null)
        }
    }

    override fun digit(d: Char) = edit(continueFromResult = false) { cur ->
        val seg = trailingNumber(cur)
        when {
            seg.count { it != '.' } >= MAX_DIGITS -> cur
            seg == "0" && d == '0' -> cur
            seg == "0" -> cur.dropLast(1) + d
            else -> cur + d
        }
    }

    override fun decimal() = edit(continueFromResult = false) { cur ->
        val seg = trailingNumber(cur)
        when {
            '.' in seg -> cur
            seg.isEmpty() -> "$cur" + "0."
            else -> "$cur."
        }
    }

    override fun operation(op: Char) = edit(continueFromResult = true) { cur ->
        val last = cur.lastOrNull()
        if (last == null) {
            if (op == '−') "−" else cur
        } else if (last == '(') {
            if (op == '−') cur + op else cur
        } else if (last in OPERATORS) {
            if (op == '−' && last in "×÷^") {
                cur + op
            } else {
                val stripped = cur.trimEnd(*OPERATOR_CHARS)
                if (stripped.isEmpty()) (if (op == '−') "−" else "") else stripped + op
            }
        } else {
            cur + op
        }
    }

    override fun function(name: String) = edit(continueFromResult = false) { cur -> "$cur$name(" }

    override fun constant(symbol: String) = edit(continueFromResult = false) { cur -> cur + symbol }

    override fun parenthesis() = edit(continueFromResult = false) { cur ->
        val open = cur.count { it == '(' } - cur.count { it == ')' }
        if (open > 0 && endsWithOperand(cur)) "$cur)" else "$cur("
    }

    override fun percent() = edit(continueFromResult = true) { cur ->
        if (endsWithOperand(cur)) "$cur%" else cur
    }

    override fun factorial() = edit(continueFromResult = true) { cur ->
        if (endsWithOperand(cur)) "$cur!" else cur
    }

    override fun suffix(text: String) = edit(continueFromResult = true) { cur ->
        if (endsWithOperand(cur)) cur + text else cur
    }

    override fun backspace() {
        state.update { s ->
            val base = s.result?.replace('-', '−') ?: s.expression
            s.copy(
                expression = ExpressionEvaluator.removeLastToken(base),
                result = null,
                committed = null,
                error = null,
            )
        }
    }

    override fun clear() {
        state.update { CalcState(scientific = it.scientific, angle = it.angle, memory = it.memory) }
    }

    override fun evaluate() {
        val s = state.value
        if (s.result != null || s.expression.isEmpty()) return
        val trimmed = ExpressionEvaluator.trimTrailing(s.expression)
        if (trimmed.isEmpty() || LONE_NUMBER.matches(trimmed)) return
        val balanced = ExpressionEvaluator.balance(trimmed)
        when (val r = ExpressionEvaluator.evaluate(balanced, s.angle)) {
            is EvalResult.Success -> {
                val plain = NumberFormatter.toPlain(r.value)
                state.update {
                    it.copy(
                        expression = plain.replace('-', '−'),
                        result = plain,
                        committed = balanced,
                        error = null,
                    )
                }
                viewModelScope.launch { history.add(balanced, plain) }
            }
            is EvalResult.Failure -> state.update { it.copy(error = r.error, errorTick = it.errorTick + 1) }
        }
    }

    override fun toggleAngle() {
        state.update { it.copy(angle = if (it.angle == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG) }
    }

    override fun toggleScientific() {
        state.update { it.copy(scientific = !it.scientific) }
    }

    // ---- Memory ---------------------------------------------------------------------------------

    /** The number currently on screen: a committed result, a lone number, or the live preview. */
    private fun currentValue(s: CalcState): Double? {
        if (s.error != null) return null
        val text = (s.result ?: s.expression).replace('-', '−')
        if (text.isEmpty()) return null
        val value = if (LONE_NUMBER.matches(text)) {
            text.replace('−', '-').toDoubleOrNull()
        } else {
            ExpressionEvaluator.preview(s.expression, s.angle)
        }
        return value?.takeIf { it.isFinite() }
    }

    override fun memoryClear() = state.update { it.copy(memory = null) }

    override fun memoryRecall() {
        val m = state.value.memory ?: return
        appendResult(NumberFormatter.toPlain(m))
    }

    override fun memoryAdd() = state.update { s ->
        val v = currentValue(s) ?: return@update s
        s.copy(memory = (s.memory ?: 0.0) + v)
    }

    override fun memorySubtract() = state.update { s ->
        val v = currentValue(s) ?: return@update s
        s.copy(memory = (s.memory ?: 0.0) - v)
    }

    override fun memoryStore() = state.update { s ->
        val v = currentValue(s) ?: return@update s
        s.copy(memory = v)
    }

    // ---- From History ---------------------------------------------------------------------------

    /** Puts a saved expression back on the display for editing. */
    fun loadExpression(expression: String) {
        state.update {
            it.copy(expression = expression.replace('-', '−'), result = null, committed = null, error = null)
        }
    }

    /** Inserts a saved result into whatever is being typed (or starts fresh after "="). */
    fun appendResult(plain: String) = edit(continueFromResult = false) { cur ->
        val value = plain.replace('-', '−')
        if (endsWithOperand(cur)) "$cur×$value" else cur + value
    }
}

private fun buildDisplay(s: CalcState, settings: NumberFormatSettings): DisplayState {
    val f = NumberFormatter(settings)
    val base = DisplayState(
        scientific = s.scientific,
        angle = s.angle,
        decimalMark = settings.decimalMark.char,
        errorTick = s.errorTick,
        memory = s.memory?.let { f.display(NumberFormatter.toPlain(it)) },
    )
    return when {
        s.result != null -> base.copy(
            primary = f.display(s.result),
            secondary = f.expression(s.committed.orEmpty()) + " =",
            copyValue = s.result,
        )
        s.error != null -> base.copy(
            primary = s.error.message,
            secondary = f.expression(s.expression),
            isError = true,
        )
        s.expression.isEmpty() -> base
        LONE_NUMBER.matches(s.expression) -> base.copy(
            primary = f.typing(s.expression),
            copyValue = s.expression.replace('−', '-').takeIf { it.any { ch -> ch in '0'..'9' } },
        )
        else -> {
            val plain = ExpressionEvaluator.preview(s.expression, s.angle)?.let { NumberFormatter.toPlain(it) }
            base.copy(
                primary = plain?.let { f.display(it) } ?: "0",
                secondary = f.expression(s.expression),
                copyValue = plain,
            )
        }
    }
}
