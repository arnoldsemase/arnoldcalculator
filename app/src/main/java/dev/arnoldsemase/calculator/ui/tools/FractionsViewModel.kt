package dev.arnoldsemase.calculator.ui.tools

import androidx.lifecycle.ViewModel
import dev.arnoldsemase.calculator.engine.Fraction
import dev.arnoldsemase.calculator.engine.FractionCalc
import dev.arnoldsemase.calculator.engine.FractionInput
import dev.arnoldsemase.calculator.engine.FractionOp
import dev.arnoldsemase.calculator.engine.FractionOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class FractionPart { WHOLE, NUM, DEN }

/** The six boxes the user can type into: side 0 is the first fraction, side 1 the second. */
enum class FractionSlot(val side: Int, val part: FractionPart) {
    A_WHOLE(0, FractionPart.WHOLE), A_NUM(0, FractionPart.NUM), A_DEN(0, FractionPart.DEN),
    B_WHOLE(1, FractionPart.WHOLE), B_NUM(1, FractionPart.NUM), B_DEN(1, FractionPart.DEN),
}

data class FractionsUiState(
    val a: FractionInput = FractionInput(),
    val b: FractionInput = FractionInput(),
    val op: FractionOp = FractionOp.PLUS,
    val active: FractionSlot = FractionSlot.A_NUM,
) {
    val outcome: FractionOutcome get() = FractionCalc.compute(a, op, b)

    fun input(side: Int): FractionInput = if (side == 0) a else b

    fun text(slot: FractionSlot): String = input(slot.side).let {
        when (slot.part) {
            FractionPart.WHOLE -> it.whole
            FractionPart.NUM -> it.num
            FractionPart.DEN -> it.den
        }
    }

    /** Boxes currently on screen, in the order the Next key visits them. */
    fun visibleSlots(): List<FractionSlot> = FractionSlot.entries.filter { slot ->
        slot.part != FractionPart.WHOLE || input(slot.side).mixed
    }

    private fun with(side: Int, change: (FractionInput) -> FractionInput) =
        if (side == 0) copy(a = change(a)) else copy(b = change(b))

    fun withText(slot: FractionSlot, value: String): FractionsUiState = with(slot.side) {
        when (slot.part) {
            FractionPart.WHOLE -> it.copy(whole = value)
            FractionPart.NUM -> it.copy(num = value)
            FractionPart.DEN -> it.copy(den = value)
        }
    }

    fun withInput(side: Int, change: (FractionInput) -> FractionInput) = with(side, change)
}

private const val MAX_DIGITS = 6

class FractionsViewModel : ViewModel() {

    private val _state = MutableStateFlow(FractionsUiState())
    val state: StateFlow<FractionsUiState> = _state.asStateFlow()

    fun select(slot: FractionSlot) = _state.update { if (slot in it.visibleSlots()) it.copy(active = slot) else it }

    fun digit(d: Char) = _state.update { s ->
        val text = s.text(s.active)
        val next = when {
            text.length >= MAX_DIGITS -> text
            text == "0" -> d.toString()
            else -> text + d
        }
        s.withText(s.active, next)
    }

    /** Deletes the last digit; on an empty box it steps back to the previous one. */
    fun backspace() = _state.update { s ->
        val text = s.text(s.active)
        if (text.isNotEmpty()) {
            s.withText(s.active, text.dropLast(1))
        } else {
            val slots = s.visibleSlots()
            val i = slots.indexOf(s.active)
            if (i > 0) s.copy(active = slots[i - 1]) else s
        }
    }

    fun next() = _state.update { s ->
        val slots = s.visibleSlots()
        val i = slots.indexOf(s.active)
        s.copy(active = slots[(i + 1) % slots.size])
    }

    fun clear() = _state.update { FractionsUiState(op = it.op) }

    fun negate() = _state.update { s -> s.withInput(s.active.side) { it.copy(negative = !it.negative) } }

    /** Shows or hides the whole-number box of the fraction being edited. */
    fun toggleMixed() = _state.update { s ->
        val side = s.active.side
        val turningOn = !s.input(side).mixed
        val whole = if (side == 0) FractionSlot.A_WHOLE else FractionSlot.B_WHOLE
        val num = if (side == 0) FractionSlot.A_NUM else FractionSlot.B_NUM
        s.withInput(side) { it.copy(mixed = turningOn, whole = if (turningOn) it.whole else "") }
            .copy(active = if (turningOn) whole else if (s.active == whole) num else s.active)
    }

    fun cycleOperation() = _state.update { s ->
        val all = FractionOp.entries
        s.copy(op = all[(s.op.ordinal + 1) % all.size])
    }

    /** "=": keeps the answer as the first fraction so the user can carry on calculating. */
    fun evaluate() = _state.update { s ->
        val outcome = s.outcome
        if (outcome !is FractionOutcome.Answer) return@update s
        FractionsUiState(a = outcome.value.toInput(), b = FractionInput(), op = s.op, active = FractionSlot.B_NUM)
    }

    private fun Fraction.toInput(): FractionInput {
        val abs = num.abs().toString()
        return if (isInteger) {
            FractionInput(negative = isNegative, mixed = true, whole = abs)
        } else {
            FractionInput(negative = isNegative, num = abs, den = den.toString())
        }
    }
}
