@file:OptIn(ExperimentalLayoutApi::class)

package dev.arnoldsemase.calculator.ui.tools

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.arnoldsemase.calculator.engine.NumberFormatter
import dev.arnoldsemase.calculator.engine.PercentLine
import dev.arnoldsemase.calculator.engine.PercentMode
import dev.arnoldsemase.calculator.engine.PercentageCalc
import dev.arnoldsemase.calculator.ui.components.ArnoldSnackbar
import dev.arnoldsemase.calculator.ui.components.SubScreenBar
import dev.arnoldsemase.calculator.ui.components.rememberHaptics
import dev.arnoldsemase.calculator.ui.settings.SettingsViewModel
import dev.arnoldsemase.calculator.ui.theme.Arnold
import dev.arnoldsemase.calculator.ui.theme.ArnoldFonts
import kotlinx.coroutines.launch

/** A piece of the question sentence: fixed words, or the number field with this index (0 = A, 1 = B). */
private sealed interface Part {
    data class Words(val text: String) : Part
    data class Field(val index: Int) : Part
}

private fun sentence(mode: PercentMode): List<Part> = when (mode) {
    PercentMode.OF -> listOf(Part.Words("What is"), Part.Field(0), Part.Words("% of"), Part.Field(1), Part.Words("?"))
    PercentMode.CHANGE -> listOf(Part.Words("From"), Part.Field(0), Part.Words("to"), Part.Field(1))
    PercentMode.IS -> listOf(Part.Field(0), Part.Words("is what % of"), Part.Field(1), Part.Words("?"))
    PercentMode.TIP -> listOf(Part.Words("Tip"), Part.Field(0), Part.Words("% on a bill of"), Part.Field(1))
    PercentMode.DISCOUNT -> listOf(Part.Words("Take"), Part.Field(0), Part.Words("% off"), Part.Field(1))
}

private val QUICK_PERCENTS = listOf("5", "10", "15", "20", "25")

@Composable
fun PercentageScreen(settingsViewModel: SettingsViewModel, onBack: () -> Unit) {
    val c = Arnold.colors
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(settings.haptics)
    val formatter = remember(settings.numberFormat) { NumberFormatter(settings.numberFormat) }
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var modeIndex by rememberSaveable { mutableIntStateOf(0) }
    var aText by rememberSaveable { mutableStateOf("15") }
    var bText by rememberSaveable { mutableStateOf("850") }
    var activeField by rememberSaveable { mutableIntStateOf(0) }

    val mode = PercentMode.entries[modeIndex]
    val result = PercentageCalc.compute(mode, aText.toDoubleOrNull(), bText.toDoubleOrNull())

    fun edit(change: (String) -> String) {
        if (activeField == 0) aText = change(aText) else bText = change(bText)
    }

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
            SubScreenBar(title = "Percentage", onBack = onBack)
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryChips(
                    names = PercentMode.entries.map { it.label },
                    selected = modeIndex,
                    onSelect = { i ->
                        if (i != modeIndex) {
                            haptics.tick()
                            modeIndex = i
                        }
                    },
                )

                FlowRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    sentence(mode).forEach { part ->
                        when (part) {
                            is Part.Words -> Text(part.text, color = c.textSecondary, fontSize = 20.sp)
                            is Part.Field -> NumberField(
                                text = if (part.index == 0) aText else bText,
                                shown = formatter.typing((if (part.index == 0) aText else bText).ifEmpty { "0" }),
                                empty = (if (part.index == 0) aText else bText).isEmpty(),
                                active = activeField == part.index,
                                onClick = { haptics.tick(); activeField = part.index },
                            )
                        }
                    }
                }

                ResultCard(result = result, mode = mode, formatter = formatter)

                if (mode == PercentMode.OF || mode == PercentMode.TIP || mode == PercentMode.DISCOUNT) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QUICK_PERCENTS.forEach { p ->
                            QuickChip(
                                label = "$p%",
                                selected = aText == p,
                                modifier = Modifier.weight(1f),
                                onClick = { haptics.tick(); aText = p },
                            )
                        }
                    }
                }

                NumberPad(
                    haptics = haptics,
                    decimalMark = settings.numberFormat.decimalMark.char,
                    onDigits = { d -> edit { typeDigits(it, d) } },
                    onDecimal = { edit { typeDecimal(it) } },
                    onBackspace = { edit { typeBackspace(it) } },
                    onClear = { aText = ""; bText = ""; activeField = 0 },
                    onNegate = { activeField = 1 - activeField },
                    onCopy = {
                        if (result == null) {
                            haptics.reject()
                        } else {
                            clipboard.setText(AnnotatedString(PercentageCalc.plain(result.value)))
                            haptics.confirm()
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar("Copied to clipboard")
                            }
                        }
                    },
                    extraLabel = "Next",
                    extraDescription = "Move to the other number",
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun NumberField(text: String, shown: String, empty: Boolean, active: Boolean, onClick: () -> Unit) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .defaultMinSize(minWidth = 76.dp, minHeight = 52.dp)
            .clip(shape)
            .background(if (active) c.accentTint(0.08f) else c.surface)
            .border(1.dp, if (active) c.accent else c.outline.copy(alpha = 0.7f), shape)
            .clickable(role = Role.Button, onClickLabel = "Edit this number", onClick = onClick)
            .padding(horizontal = 14.dp)
            .semantics { contentDescription = if (empty) "empty" else text },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = shown,
            color = if (empty) c.textSecondary else c.text,
            fontFamily = ArnoldFonts.Display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun ResultCard(
    result: dev.arnoldsemase.calculator.engine.PercentResult?,
    mode: PercentMode,
    formatter: NumberFormatter,
) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(24.dp)

    fun text(value: Double, percent: Boolean, signed: Boolean = false): String {
        val body = formatter.display(PercentageCalc.plain(value)) + if (percent) " %" else ""
        return if (signed && value > 0.0) "+$body" else body
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.accentTint(0.2f), c.accentTint(0.07f))))
            .border(1.dp, c.accent.copy(alpha = 0.35f), shape)
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(result?.title ?: "Result", color = c.accentText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = result?.let { text(it.value, it.isPercent, signed = mode == PercentMode.CHANGE) } ?: "—",
            color = c.text,
            fontFamily = ArnoldFonts.Display,
            fontWeight = FontWeight.Medium,
            fontSize = 52.sp,
            letterSpacing = (-1).sp,
            maxLines = 1,
        )
        if (result == null) {
            Text(
                text = "Enter both numbers",
                color = c.textSecondary,
                fontSize = 13.sp,
            )
        } else {
            result.lines.forEach { line -> ExtraLine(line, text(line.value, line.isPercent)) }
        }
    }
}

@Composable
private fun ExtraLine(line: PercentLine, value: String) {
    val c = Arnold.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(line.label, color = c.textSecondary, fontSize = 13.sp)
        Text(value, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QuickChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(44.dp)
            .clip(shape)
            .background(if (selected) c.accentTint(0.2f) else c.surface)
            .border(1.dp, if (selected) c.accent.copy(alpha = 0.4f) else c.outline.copy(alpha = 0.6f), shape)
            .clickable(role = Role.Button, onClickLabel = "Use $label", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) c.accentText else c.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
