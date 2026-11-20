package dev.arnoldsemase.calculator.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.arnoldsemase.calculator.engine.ConvUnit
import dev.arnoldsemase.calculator.engine.NumberFormatter
import dev.arnoldsemase.calculator.engine.UnitConverter
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
fun UnitsScreen(settingsViewModel: SettingsViewModel, onBack: () -> Unit) {
    val c = Arnold.colors
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(settings.haptics)
    val formatter = remember(settings.numberFormat) { NumberFormatter(settings.numberFormat) }
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var categoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var fromIndex by rememberSaveable { mutableIntStateOf(UnitConverter.categories[0].defaultFrom) }
    var toIndex by rememberSaveable { mutableIntStateOf(UnitConverter.categories[0].defaultTo) }
    var input by rememberSaveable { mutableStateOf("") }

    val category = UnitConverter.categories[categoryIndex]
    val from = category.units[fromIndex]
    val to = category.units[toIndex]

    val value = input.toDoubleOrNull() ?: 0.0
    val resultPlain = NumberFormatter.toPlain(UnitConverter.convert(value, from, to))
    val oneUnit = NumberFormatter.toPlain(UnitConverter.convert(1.0, from, to))

    val copy: () -> Unit = {
        clipboard.setText(AnnotatedString(resultPlain))
        haptics.confirm()
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar("Copied to clipboard")
        }
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
            SubScreenBar(title = "Units", onBack = onBack)
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryChips(
                    names = UnitConverter.categories.map { it.name },
                    selected = categoryIndex,
                    onSelect = { i ->
                        if (i != categoryIndex) {
                            haptics.tick()
                            categoryIndex = i
                            fromIndex = UnitConverter.categories[i].defaultFrom
                            toIndex = UnitConverter.categories[i].defaultTo
                        }
                    },
                )

                Box {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ValueCard(
                            label = "From",
                            units = category.units,
                            unit = from,
                            valueText = formatter.typing(input.ifEmpty { "0" }),
                            active = true,
                            onPickUnit = { fromIndex = it },
                        )
                        ValueCard(
                            label = "To",
                            units = category.units,
                            unit = to,
                            valueText = formatter.display(resultPlain),
                            active = false,
                            onPickUnit = { toIndex = it },
                        )
                    }
                    SwapButton(
                        Modifier
                            .align(Alignment.Center)
                            .offset(y = 0.dp),
                    ) {
                        haptics.confirm()
                        val f = fromIndex
                        fromIndex = toIndex
                        toIndex = f
                    }
                }

                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "1 ${from.symbol} = ${formatter.display(oneUnit)} ${to.symbol}",
                        color = c.textSecondary,
                        fontSize = 12.sp,
                    )
                    Text(text = "${from.name} → ${to.name}", color = c.textSecondary, fontSize = 12.sp, maxLines = 1)
                }

                NumberPad(
                    haptics = haptics,
                    decimalMark = settings.numberFormat.decimalMark.char,
                    onDigits = { input = typeDigits(input, it) },
                    onDecimal = { input = typeDecimal(input) },
                    onBackspace = { input = typeBackspace(input) },
                    onClear = { input = "" },
                    onNegate = { input = typeNegate(input) },
                    onCopy = copy,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
internal fun CategoryChips(names: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = Arnold.colors
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        names.forEachIndexed { i, name ->
            val on = i == selected
            val shape = RoundedCornerShape(50)
            Box(
                Modifier
                    .height(40.dp)
                    .clip(shape)
                    .background(if (on) c.accentTint(0.2f) else c.surface)
                    .border(1.dp, if (on) c.accent.copy(alpha = 0.35f) else c.outline.copy(alpha = 0.6f), shape)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(i) })
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name,
                    color = if (on) c.accentText else c.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ValueCard(
    label: String,
    units: List<ConvUnit>,
    unit: ConvUnit,
    valueText: String,
    active: Boolean,
    onPickUnit: (Int) -> Unit,
) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(22.dp)
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (active) c.accentTint(0.08f) else c.surface)
            .border(1.dp, if (active) c.accent else c.outline.copy(alpha = 0.6f), shape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box {
                val pill = RoundedCornerShape(12.dp)
                Row(
                    Modifier
                        .height(40.dp)
                        .clip(pill)
                        .background(c.surfaceHigh)
                        .clickable(role = Role.Button, onClickLabel = "Change $label unit") { open = true }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "${unit.name} · ${unit.symbol}",
                        color = c.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Text("▾", color = c.textSecondary, fontSize = 12.sp)
                }
                DropdownMenu(
                    expanded = open,
                    onDismissRequest = { open = false },
                    modifier = Modifier.background(c.surfaceHigh),
                ) {
                    units.forEachIndexed { i, u ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${u.name} · ${u.symbol}",
                                    color = if (u === unit) c.accentText else c.text,
                                    fontWeight = if (u === unit) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            onClick = {
                                open = false
                                onPickUnit(i)
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(label, color = c.textSecondary, fontSize = 12.sp)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState(), reverseScrolling = true)
                .semantics { contentDescription = "$label ${unit.name}: $valueText" },
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = valueText,
                style = TextStyle(
                    color = if (active) c.text else c.accentText,
                    fontFamily = ArnoldFonts.Display,
                    fontWeight = FontWeight.Medium,
                    fontSize = 38.sp,
                    letterSpacing = (-0.5).sp,
                    fontFeatureSettings = "tnum",
                ),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun SwapButton(modifier: Modifier, onClick: () -> Unit) {
    val c = Arnold.colors
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(c.accentSecondary, c.accent)))
            .clickable(role = Role.Button, onClickLabel = "Swap units", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("⇅", color = c.onAccent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}
