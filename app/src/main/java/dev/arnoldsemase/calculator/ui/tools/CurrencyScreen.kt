@file:OptIn(ExperimentalFoundationApi::class)

package dev.arnoldsemase.calculator.ui.tools

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import dev.arnoldsemase.calculator.engine.CurrencyConverter
import dev.arnoldsemase.calculator.engine.CurrencyNames
import dev.arnoldsemase.calculator.engine.NumberFormatter
import dev.arnoldsemase.calculator.ui.components.ArnoldIconButton
import dev.arnoldsemase.calculator.ui.components.ArnoldSnackbar
import dev.arnoldsemase.calculator.ui.components.SubScreenBar
import dev.arnoldsemase.calculator.ui.components.rememberHaptics
import dev.arnoldsemase.calculator.ui.settings.SettingsViewModel
import dev.arnoldsemase.calculator.ui.theme.Arnold
import dev.arnoldsemase.calculator.ui.theme.ArnoldFonts
import kotlinx.coroutines.launch

private val GREEN = androidx.compose.ui.graphics.Color(0xFF5BD68A)

@Composable
fun CurrencyScreen(viewModel: CurrencyViewModel, settingsViewModel: SettingsViewModel, onBack: () -> Unit) {
    val c = Arnold.colors
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(settings.haptics)
    val formatter = remember(settings.numberFormat) { NumberFormatter(settings.numberFormat) }
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var picking by remember { mutableStateOf(false) }

    fun say(message: String) = scope.launch {
        snackbar.currentSnackbarData?.dismiss()
        snackbar.showSnackbar(message)
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
            SubScreenBar(title = "Currency", onBack = onBack) {
                ArnoldIconButton(onClick = { haptics.tick(); viewModel.refresh() }, description = "Refresh rates") {
                    Text("↻", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatusLine(ui)

                ui.selected.forEach { code ->
                    val active = code == ui.active
                    val value = ui.valueOf(code)
                    val valueText = when {
                        active -> formatter.typing(ui.input.ifEmpty { "0" })
                        value != null -> formatter.display(CurrencyConverter.moneyPlain(value))
                        else -> "—"
                    }
                    val rate = ui.rateFromActive(code)
                    val sub = when {
                        active -> "Base · tap another currency to switch"
                        rate != null -> "1 ${ui.active} = ${formatter.display(CurrencyConverter.ratePlain(rate))} $code"
                        else -> "Rate not available"
                    }
                    CurrencyRow(
                        code = code,
                        subtitle = sub,
                        valueText = valueText,
                        active = active,
                        onClick = { haptics.tick(); viewModel.select(code) },
                        onLongClick = {
                            if (viewModel.remove(code)) {
                                haptics.confirm()
                                say("$code removed")
                            } else if (active) {
                                say("Switch to another currency to remove $code")
                            } else {
                                say("Keep at least two currencies")
                            }
                        },
                    )
                }

                AddButton(onClick = {
                    if (ui.table == null) {
                        say("Rates download once you're online")
                    } else {
                        picking = true
                    }
                })

                NumberPad(
                    haptics = haptics,
                    decimalMark = settings.numberFormat.decimalMark.char,
                    onDigits = viewModel::digits,
                    onDecimal = viewModel::decimal,
                    onBackspace = viewModel::backspace,
                    onClear = viewModel::clear,
                    onNegate = viewModel::negate,
                    onCopy = {
                        clipboard.setText(AnnotatedString(ui.input.ifEmpty { "0" }))
                        haptics.confirm()
                        say("Copied ${ui.active} amount")
                    },
                )
                Text(
                    text = "Exchange rates by open.er-api.com. Long-press a currency to remove it.",
                    color = c.textSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (picking) {
        val table = ui.table
        val options = table?.rates?.keys?.filter { it !in ui.selected }?.sorted().orEmpty()
        AlertDialog(
            onDismissRequest = { picking = false },
            containerColor = c.surface,
            title = { Text("Add currency", color = c.text, fontFamily = ArnoldFonts.Display, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(options, key = { it }) { code ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) {
                                    viewModel.add(code)
                                    picking = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(code, color = c.accentText, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 44.dp))
                            Text(CurrencyNames.of(code).takeIf { it != code } ?: "", color = c.text, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picking = false }) { Text("Close", color = c.accentText) } },
        )
    }
}

@Composable
private fun StatusLine(ui: CurrencyUiState) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(14.dp)
    val table = ui.table
    val (dot, text) = when {
        ui.refreshing -> c.textSecondary to "Updating rates…"
        table != null && ui.failed ->
            c.accent to "Couldn't update · using rates from ${CurrencyConverter.ago(System.currentTimeMillis(), table.fetchedAtMillis)}"
        table != null ->
            GREEN to "Rates updated ${CurrencyConverter.ago(System.currentTimeMillis(), table.fetchedAtMillis)} · works offline"
        ui.failed -> c.accent to "No rates yet. Connect to the internet to download them."
        else -> c.textSecondary to "Loading rates…"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outline.copy(alpha = 0.6f), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Text(text, color = c.textSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun CurrencyRow(
    code: String,
    subtitle: String,
    valueText: String,
    active: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(shape)
            .background(if (active) c.accentTint(0.1f) else c.surface)
            .border(1.dp, if (active) c.accent else c.outline.copy(alpha = 0.6f), shape)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = "Use $code as the amount you type",
                onLongClickLabel = "Remove $code",
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(c.surfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(code, color = c.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(CurrencyNames.of(code), color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(subtitle, color = c.textSecondary, fontSize = 12.sp, maxLines = 1)
        }
        Box(
            Modifier
                .widthIn(max = 160.dp)
                .horizontalScroll(rememberScrollState(), reverseScrolling = true),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = valueText,
                style = TextStyle(
                    color = if (active) c.text else c.accentText,
                    fontFamily = ArnoldFonts.Display,
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                    fontFeatureSettings = "tnum",
                ),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .border(1.dp, c.outline, shape)
            .clickable(role = Role.Button, onClickLabel = "Add a currency", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("+ Add currency", color = c.accentText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
