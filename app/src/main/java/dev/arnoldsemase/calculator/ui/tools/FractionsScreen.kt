@file:OptIn(ExperimentalLayoutApi::class)

package dev.arnoldsemase.calculator.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.arnoldsemase.calculator.engine.Fraction
import dev.arnoldsemase.calculator.engine.FractionInput
import dev.arnoldsemase.calculator.engine.FractionOutcome
import dev.arnoldsemase.calculator.ui.calculator.CalcKey
import dev.arnoldsemase.calculator.ui.calculator.KeyLabel
import dev.arnoldsemase.calculator.ui.calculator.KeyStyle
import dev.arnoldsemase.calculator.ui.components.ArnoldSnackbar
import dev.arnoldsemase.calculator.ui.components.BackspaceGlyph
import dev.arnoldsemase.calculator.ui.components.Haptics
import dev.arnoldsemase.calculator.ui.components.SubScreenBar
import dev.arnoldsemase.calculator.ui.components.rememberHaptics
import dev.arnoldsemase.calculator.ui.settings.SettingsViewModel
import dev.arnoldsemase.calculator.ui.theme.Arnold
import dev.arnoldsemase.calculator.ui.theme.ArnoldFonts
import kotlinx.coroutines.launch

@Composable
fun FractionsScreen(viewModel: FractionsViewModel, settingsViewModel: SettingsViewModel, onBack: () -> Unit) {
    val c = Arnold.colors
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(settings.haptics)
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val outcome = ui.outcome

    Scaffold(
        containerColor = c.background,
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = { SnackbarHost(snackbar) { ArnoldSnackbar(it) } },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SubScreenBar(title = "Fractions", onBack = onBack)
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val shape = RoundedCornerShape(24.dp)
                val compact = ui.a.mixed || ui.b.mixed
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(c.surface)
                        .border(1.dp, c.outline.copy(alpha = 0.6f), shape)
                        .padding(horizontal = 8.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OperandEditor(0, ui, compact, onSelect = { haptics.tick(); viewModel.select(it) })
                    OperationButton(ui.op.symbol, "Operation: ${ui.op.spoken}. Tap to change") {
                        haptics.tick()
                        viewModel.cycleOperation()
                    }
                    OperandEditor(1, ui, compact, onSelect = { haptics.tick(); viewModel.select(it) })
                }

                ResultCard(
                    outcome = outcome,
                    onCopy = {
                        val answer = outcome as? FractionOutcome.Answer
                        if (answer == null) {
                            haptics.reject()
                        } else {
                            clipboard.setText(AnnotatedString(answer.value.mixedText()))
                            haptics.confirm()
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar("Copied ${answer.value.mixedText()}")
                            }
                        }
                    },
                )

                FractionPad(haptics, viewModel)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ---- Input -----------------------------------------------------------------------------------------

@Composable
private fun OperandEditor(
    side: Int,
    ui: FractionsUiState,
    compact: Boolean,
    onSelect: (FractionSlot) -> Unit,
) {
    val c = Arnold.colors
    val input = ui.input(side)
    val slots = FractionSlot.entries.filter { it.side == side }
    val fieldWidth = if (compact) 48.dp else 64.dp
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (input.negative) {
            Text("−", color = c.accentText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        if (input.mixed) {
            DigitBox(ui.text(slots[0]), ui.active == slots[0], fieldWidth, "Whole number") { onSelect(slots[0]) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DigitBox(ui.text(slots[1]), ui.active == slots[1], fieldWidth, "Numerator") { onSelect(slots[1]) }
            Box(Modifier.width(fieldWidth).height(2.dp).background(c.text, RoundedCornerShape(1.dp)))
            DigitBox(ui.text(slots[2]), ui.active == slots[2], fieldWidth, "Denominator") { onSelect(slots[2]) }
        }
    }
}

@Composable
private fun DigitBox(text: String, active: Boolean, width: Dp, label: String, onClick: () -> Unit) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(width = width, height = 46.dp)
            .clip(shape)
            .background(c.surfaceHigh)
            .border(1.dp, if (active) c.accent else c.outline.copy(alpha = 0.6f), shape)
            .clickable(role = Role.Button, onClickLabel = "Edit $label", onClick = onClick)
            .semantics { contentDescription = "$label: ${text.ifEmpty { "empty" }}" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.ifEmpty { "·" },
            color = if (text.isEmpty()) c.textSecondary else c.text,
            fontFamily = ArnoldFonts.Display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun OperationButton(symbol: String, description: String, onClick: () -> Unit) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .size(48.dp)
            .clip(shape)
            .background(c.accentTint(0.2f))
            .border(1.dp, c.accent.copy(alpha = 0.35f), shape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = c.accentText, fontSize = 28.sp, fontWeight = FontWeight.Medium)
    }
}

// ---- Result ----------------------------------------------------------------------------------------

@Composable
private fun StackedFraction(num: String, den: String, size: Int, color: Color) {
    Column(Modifier.width(IntrinsicSize.Max), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(num, color = color, fontSize = size.sp, fontFamily = ArnoldFonts.Display, fontWeight = FontWeight.Medium)
        Box(Modifier.fillMaxWidth().height(2.dp).background(color, RoundedCornerShape(1.dp)))
        Text(den, color = color, fontSize = size.sp, fontFamily = ArnoldFonts.Display, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MixedDisplay(value: Fraction, size: Int) {
    val c = Arnold.colors
    val (whole, rest) = value.mixedParts()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (value.isNegative) Text("−", color = c.text, fontSize = size.sp, fontFamily = ArnoldFonts.Display)
        if (rest == null || whole.signum() > 0) {
            Text(
                text = whole.toString(),
                color = c.text,
                fontSize = (size * 1.3f).toInt().sp,
                fontFamily = ArnoldFonts.Display,
                fontWeight = FontWeight.Medium,
            )
        }
        if (rest != null) StackedFraction(rest.num.toString(), rest.den.toString(), size, c.text)
    }
}

@Composable
private fun ResultCard(outcome: FractionOutcome, onCopy: () -> Unit) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(24.dp)
    val answer = outcome as? FractionOutcome.Answer
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.accentTint(0.2f), c.accentTint(0.07f))))
            .border(1.dp, c.accent.copy(alpha = 0.35f), shape)
            .clickable(enabled = answer != null, role = Role.Button, onClickLabel = "Copy result", onClick = onCopy)
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = answer?.let { "Result ${it.value.mixedText()}" } ?: "No result yet"
            },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = if (outcome is FractionOutcome.Problem) "Can't calculate" else "Result",
            color = c.accentText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        when (outcome) {
            is FractionOutcome.Answer -> {
                MixedDisplay(outcome.value, size = 30)
                Text(outcome.step, color = c.textSecondary, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val v = outcome.value
                    if (!v.isInteger && v.mixedParts().first.signum() > 0) Tag("Improper ${v.improper()}")
                    if (!v.isInteger) Tag("Decimal ${if (v.isTerminating) "" else "≈ "}${v.decimalPlain()}")
                    outcome.unreduced?.let { Tag("Simplified from $it") }
                }
            }
            is FractionOutcome.Problem -> Text(outcome.message, color = c.text, fontSize = 17.sp)
            FractionOutcome.Waiting -> {
                Text("—", color = c.text, fontSize = 44.sp, fontFamily = ArnoldFonts.Display)
                Text("Fill in both fractions", color = c.textSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(50)
    Text(
        text = text,
        color = c.text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outline.copy(alpha = 0.7f), shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

// ---- Keypad ----------------------------------------------------------------------------------------

@Composable
private fun RowScope.FKey(label: String, style: KeyStyle, description: String, haptics: Haptics, onClick: () -> Unit) {
    CalcKey(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        style = style,
        description = description,
        onClick = {
            if (style == KeyStyle.Equals) haptics.confirm() else haptics.tick()
            onClick()
        },
    ) { KeyLabel(label, style) }
}

@Composable
private fun FractionPad(haptics: Haptics, vm: FractionsViewModel) {
    val c = Arnold.colors
    val gap = 10.dp
    val row = Modifier
        .fillMaxWidth()
        .height(58.dp)
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(row, horizontalArrangement = Arrangement.spacedBy(gap)) {
            FKey("7", KeyStyle.Number, "7", haptics) { vm.digit('7') }
            FKey("8", KeyStyle.Number, "8", haptics) { vm.digit('8') }
            FKey("9", KeyStyle.Number, "9", haptics) { vm.digit('9') }
            CalcKey(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                style = KeyStyle.Utility,
                description = "Backspace. Long press to clear everything",
                onClick = { haptics.tick(); vm.backspace() },
                onLongClick = { haptics.confirm(); vm.clear() },
            ) { BackspaceGlyph(c.text) }
        }
        Row(row, horizontalArrangement = Arrangement.spacedBy(gap)) {
            FKey("4", KeyStyle.Number, "4", haptics) { vm.digit('4') }
            FKey("5", KeyStyle.Number, "5", haptics) { vm.digit('5') }
            FKey("6", KeyStyle.Number, "6", haptics) { vm.digit('6') }
            FKey("▸", KeyStyle.Utility, "Next box", haptics) { vm.next() }
        }
        Row(row, horizontalArrangement = Arrangement.spacedBy(gap)) {
            FKey("1", KeyStyle.Number, "1", haptics) { vm.digit('1') }
            FKey("2", KeyStyle.Number, "2", haptics) { vm.digit('2') }
            FKey("3", KeyStyle.Number, "3", haptics) { vm.digit('3') }
            FKey("a b/c", KeyStyle.Utility, "Mixed number: show or hide the whole part", haptics) { vm.toggleMixed() }
        }
        Row(row, horizontalArrangement = Arrangement.spacedBy(gap)) {
            FKey("0", KeyStyle.Number, "0", haptics) { vm.digit('0') }
            FKey("AC", KeyStyle.Utility, "Clear all", haptics) { vm.clear() }
            FKey("±", KeyStyle.Utility, "Change sign", haptics) { vm.negate() }
            FKey("=", KeyStyle.Equals, "Equals. Use the answer as the first fraction", haptics) { vm.evaluate() }
        }
    }
}
