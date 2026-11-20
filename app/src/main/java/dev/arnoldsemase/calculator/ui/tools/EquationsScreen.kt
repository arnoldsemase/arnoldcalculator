@file:OptIn(ExperimentalLayoutApi::class)

package dev.arnoldsemase.calculator.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.arnoldsemase.calculator.engine.EquationOutcome
import dev.arnoldsemase.calculator.engine.Solution
import dev.arnoldsemase.calculator.engine.WorkingStep
import dev.arnoldsemase.calculator.ui.calculator.CalcKey
import dev.arnoldsemase.calculator.ui.calculator.KeyLabel
import dev.arnoldsemase.calculator.ui.calculator.KeyStyle
import dev.arnoldsemase.calculator.ui.components.ArnoldSnackbar
import dev.arnoldsemase.calculator.ui.components.BackspaceGlyph
import dev.arnoldsemase.calculator.ui.components.Haptics
import dev.arnoldsemase.calculator.ui.components.SegmentedChoice
import dev.arnoldsemase.calculator.ui.components.SubScreenBar
import dev.arnoldsemase.calculator.ui.components.rememberHaptics
import dev.arnoldsemase.calculator.ui.settings.SettingsViewModel
import dev.arnoldsemase.calculator.ui.theme.Arnold
import dev.arnoldsemase.calculator.ui.theme.ArnoldFonts
import kotlinx.coroutines.launch

@Composable
fun EquationsScreen(viewModel: EquationsViewModel, settingsViewModel: SettingsViewModel, onBack: () -> Unit) {
    val c = Arnold.colors
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(settings.haptics)
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val outcome = remember(ui.mode, ui.linear, ui.quadratic, ui.first, ui.second) { ui.outcome }

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
            SubScreenBar(title = "Equations", onBack = onBack)
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SegmentedChoice(
                    options = EquationMode.entries.map { it to it.label },
                    selected = ui.mode,
                    onSelect = { haptics.tick(); viewModel.setMode(it) },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (ui.mode == EquationMode.SYSTEM) {
                    EquationField("Equation 1", ui.first, !ui.secondActive, "2x + y = 5") { viewModel.selectField(false) }
                    EquationField("Equation 2", ui.second, ui.secondActive, "x − y = 1") { viewModel.selectField(true) }
                } else {
                    EquationField(
                        label = "Equation",
                        text = ui.activeText,
                        active = true,
                        placeholder = if (ui.mode == EquationMode.LINEAR) "2x + 3 = 11" else "x² − 5x + 6 = 0",
                    ) {}
                }

                ResultCard(
                    outcome = outcome,
                    hint = ui.mode.hint,
                    onCopy = {
                        val text = copyText(outcome)
                        if (text == null) {
                            haptics.reject()
                        } else {
                            clipboard.setText(AnnotatedString(text))
                            haptics.confirm()
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar("Copied to clipboard")
                            }
                        }
                    },
                )

                val steps = when (outcome) {
                    is EquationOutcome.Solved -> outcome.steps
                    is EquationOutcome.NoUniqueSolution -> outcome.steps
                    else -> emptyList()
                }
                if (steps.isNotEmpty()) StepsCard(steps)

                EquationKeys(haptics, viewModel, systemMode = ui.mode == EquationMode.SYSTEM)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private fun copyText(outcome: EquationOutcome): String? = when (outcome) {
    is EquationOutcome.Solved -> outcome.solutions.joinToString(", ") { "${it.name} = ${it.exact}" }
    is EquationOutcome.NoUniqueSolution -> outcome.headline
    else -> null
}

// ---- Equation input --------------------------------------------------------------------------------

@Composable
private fun EquationField(label: String, text: String, active: Boolean, placeholder: String, onClick: () -> Unit) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, if (active) c.accent else c.outline.copy(alpha = 0.6f), shape)
            .clickable(role = Role.Button, onClickLabel = "Type into $label", onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .semantics { contentDescription = "$label: ${text.ifEmpty { "empty" }}" },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = c.textSecondary, fontSize = 12.sp)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState(), reverseScrolling = true),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (text.isEmpty()) {
                Text(
                    placeholder,
                    style = TextStyle(
                        color = c.textSecondary.copy(alpha = 0.6f),
                        fontFamily = ArnoldFonts.Display,
                        fontSize = 26.sp,
                    ),
                    maxLines = 1,
                    softWrap = false,
                )
            } else {
                Text(
                    text,
                    style = TextStyle(
                        color = c.text,
                        fontFamily = ArnoldFonts.Display,
                        fontWeight = FontWeight.Medium,
                        fontSize = 26.sp,
                    ),
                    maxLines = 1,
                    softWrap = false,
                )
            }
            if (active) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(c.accent, RoundedCornerShape(1.dp)),
                )
            }
        }
    }
}

// ---- Result and steps ------------------------------------------------------------------------------

@Composable
private fun ResultCard(outcome: EquationOutcome, hint: String, onCopy: () -> Unit) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(24.dp)
    val copyable = outcome is EquationOutcome.Solved || outcome is EquationOutcome.NoUniqueSolution
    val title = when (outcome) {
        is EquationOutcome.Solved -> if (outcome.solutions.size > 1) "Solutions" else "Solution"
        is EquationOutcome.Problem -> "Can't solve"
        else -> "Solution"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.accentTint(0.2f), c.accentTint(0.07f))))
            .border(1.dp, c.accent.copy(alpha = 0.35f), shape)
            .clickable(enabled = copyable, role = Role.Button, onClickLabel = "Copy solutions", onClick = onCopy)
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = c.accentText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        when (outcome) {
            is EquationOutcome.Solved -> {
                outcome.solutions.forEach { SolutionRow(it) }
                Text(outcome.note, color = c.textSecondary, fontSize = 13.sp)
            }
            is EquationOutcome.NoUniqueSolution -> {
                Text(
                    outcome.headline,
                    color = c.text,
                    fontFamily = ArnoldFonts.Display,
                    fontWeight = FontWeight.Medium,
                    fontSize = 30.sp,
                )
                Text(outcome.detail, color = c.textSecondary, fontSize = 13.sp)
            }
            is EquationOutcome.Problem -> Text(outcome.message, color = c.text, fontSize = 17.sp)
            EquationOutcome.Waiting -> {
                Text("—", color = c.text, fontSize = 44.sp, fontFamily = ArnoldFonts.Display)
                Text(hint, color = c.textSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SolutionRow(s: Solution) {
    val c = Arnold.colors
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "${s.name} = ${s.exact}",
            color = c.text,
            fontFamily = ArnoldFonts.Display,
            fontWeight = FontWeight.Medium,
            fontSize = 28.sp,
        )
        s.approx?.let { Text(it, color = c.textSecondary, fontSize = 15.sp) }
    }
}

@Composable
private fun StepsCard(steps: List<WorkingStep>) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outline.copy(alpha = 0.6f), shape)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        steps.forEachIndexed { i, step ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${i + 1} · ${step.label}",
                    color = c.textSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(0.45f),
                )
                Text(
                    step.value,
                    color = c.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(0.55f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
            if (i < steps.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.outline.copy(alpha = 0.5f)))
            }
        }
    }
}

// ---- Keys ------------------------------------------------------------------------------------------

@Composable
private fun RowScope.EKey(
    label: String,
    style: KeyStyle,
    description: String,
    haptics: Haptics,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val c = Arnold.colors
    CalcKey(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        style = style,
        description = if (enabled) description else "$description. Only used in System mode",
        onClick = {
            if (enabled) {
                if (style == KeyStyle.Equals) haptics.confirm() else haptics.tick()
                onClick()
            }
        },
    ) {
        if (enabled) {
            KeyLabel(label, style)
        } else {
            Text(
                label,
                color = c.textSecondary.copy(alpha = 0.4f),
                fontFamily = ArnoldFonts.Display,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun EquationKeys(haptics: Haptics, vm: EquationsViewModel, systemMode: Boolean) {
    val c = Arnold.colors
    val gap = 8.dp
    val row = Modifier
        .fillMaxWidth()
        .height(54.dp)
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(row, horizontalArrangement = Arrangement.spacedBy(gap)) {
            EKey("x", KeyStyle.Science, "x", haptics) { vm.type("x") }
            EKey("y", KeyStyle.Science, "y", haptics, enabled = systemMode) { vm.type("y") }
            EKey("x²", KeyStyle.Science, "x squared", haptics) { vm.type("x²") }
            EKey("xʸ", KeyStyle.Science, "Power", haptics) { vm.type("^") }
            EKey("(", KeyStyle.Utility, "Open bracket", haptics) { vm.type("(") }
            EKey(")", KeyStyle.Utility, "Close bracket", haptics) { vm.type(")") }
        }
        Row(row, horizontalArrangement = Arrangement.spacedBy(gap)) {
            EKey("7", KeyStyle.Number, "7", haptics) { vm.type("7") }
            EKey("8", KeyStyle.Number, "8", haptics) { vm.type("8") }
            EKey("9", KeyStyle.Number, "9", haptics) { vm.type("9") }
            EKey("+", KeyStyle.Operator, "Plus", haptics) { vm.type("+") }
            EKey("−", KeyStyle.Operator, "Minus", haptics) { vm.type("−") }
            CalcKey(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                style = KeyStyle.Utility,
                description = "Backspace. Long press to clear",
                onClick = { haptics.tick(); vm.backspace() },
                onLongClick = { haptics.confirm(); vm.clear() },
            ) { BackspaceGlyph(c.text) }
        }
        Row(row, horizontalArrangement = Arrangement.spacedBy(gap)) {
            EKey("4", KeyStyle.Number, "4", haptics) { vm.type("4") }
            EKey("5", KeyStyle.Number, "5", haptics) { vm.type("5") }
            EKey("6", KeyStyle.Number, "6", haptics) { vm.type("6") }
            EKey("×", KeyStyle.Operator, "Times", haptics) { vm.type("×") }
            EKey("÷", KeyStyle.Operator, "Divide", haptics) { vm.type("÷") }
            EKey("=", KeyStyle.Equals, "Equals sign", haptics) { vm.type("=") }
        }
        Row(row, horizontalArrangement = Arrangement.spacedBy(gap)) {
            EKey("1", KeyStyle.Number, "1", haptics) { vm.type("1") }
            EKey("2", KeyStyle.Number, "2", haptics) { vm.type("2") }
            EKey("3", KeyStyle.Number, "3", haptics) { vm.type("3") }
            EKey("0", KeyStyle.Number, "0", haptics) { vm.type("0") }
            EKey(".", KeyStyle.Number, "Decimal point", haptics) { vm.type(".") }
            EKey("AC", KeyStyle.Utility, "Clear", haptics) { vm.clear() }
        }
    }
}
