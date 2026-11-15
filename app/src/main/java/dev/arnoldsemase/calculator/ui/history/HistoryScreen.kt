@file:OptIn(ExperimentalFoundationApi::class)

package dev.arnoldsemase.calculator.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.arnoldsemase.calculator.data.HistoryEntry
import dev.arnoldsemase.calculator.ui.components.ArnoldSnackbar
import dev.arnoldsemase.calculator.ui.components.SubScreenBar
import dev.arnoldsemase.calculator.ui.theme.Arnold
import dev.arnoldsemase.calculator.ui.theme.ArnoldFonts
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onUseResult: (String) -> Unit,
    onEditExpression: (String) -> Unit,
) {
    val c = Arnold.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }

    val groups = (state as? HistoryUiState.Loaded)?.groups.orEmpty()

    fun toast(message: String) = scope.launch {
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
            SubScreenBar(title = "History", onBack = onBack) {
                if (groups.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) {
                        Text("Clear all", color = c.accentText, fontWeight = FontWeight.Bold)
                    }
                }
            }

            when {
                state is HistoryUiState.Loading -> Box(Modifier.fillMaxSize())
                groups.isEmpty() -> EmptyHistory()
                else -> LazyColumn(
                    Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    groups.forEach { group ->
                        stickyHeader(key = "h-${group.title}") {
                            Text(
                                text = group.title.uppercase(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(c.background)
                                    .padding(top = 12.dp, bottom = 6.dp, start = 4.dp),
                                color = c.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                            )
                        }
                        items(group.items, key = { it.entry.id }) { item ->
                            HistoryCard(
                                item = item,
                                modifier = Modifier.animateItem(),
                                onCopy = {
                                    clipboard.setText(AnnotatedString(item.entry.result))
                                    toast("Result copied")
                                },
                                onUseResult = { onUseResult(item.entry.result) },
                                onEdit = { onEditExpression(item.entry.expression) },
                                onDelete = {
                                    viewModel.delete(item.entry)
                                    scope.launch {
                                        snackbar.currentSnackbarData?.dismiss()
                                        val r = snackbar.showSnackbar(
                                            message = "Entry deleted",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short,
                                        )
                                        if (r == SnackbarResult.ActionPerformed) viewModel.undoDelete(item.entry)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = c.surface,
            title = { Text("Clear all history?", color = c.text, fontWeight = FontWeight.Bold) },
            text = { Text("Every saved calculation will be removed from this device.", color = c.textSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAll(); confirmClear = false }) {
                    Text("Clear all", color = c.accentText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = c.textSecondary) }
            },
        )
    }
}

@Composable
private fun HistoryCard(
    item: HistoryItem,
    onCopy: () -> Unit,
    onUseResult: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outline.copy(alpha = 0.6f), shape)
            .padding(start = 16.dp, end = 10.dp, top = 14.dp, bottom = 8.dp),
    ) {
        Row(Modifier.padding(end = 6.dp), verticalAlignment = Alignment.Top) {
            Text(
                text = item.expressionText,
                modifier = Modifier.weight(1f),
                color = c.textSecondary,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.timeText,
                modifier = Modifier.padding(start = 12.dp),
                color = c.textSecondary.copy(alpha = 0.8f),
                fontSize = 12.sp,
            )
        }
        Text(
            text = "= ${item.resultText}",
            modifier = Modifier.padding(top = 2.dp, end = 6.dp),
            color = c.text,
            fontFamily = ArnoldFonts.Display,
            fontWeight = FontWeight.Light,
            fontSize = 30.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ActionChip("Copy", "Copy result", primary = false, onClick = onCopy)
            ActionChip("Use result", "Use result in calculator", primary = true, onClick = onUseResult)
            ActionChip("Edit", "Edit expression in calculator", primary = false, onClick = onEdit)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onDelete)
                    .semantics { contentDescription = "Delete entry" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, description: String, primary: Boolean, onClick: () -> Unit) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(if (primary) c.accentTint(0.18f) else c.surfaceHigh)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (primary) c.accentText else c.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyHistory() {
    val c = Arnold.colors
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(c.surface)
                .border(1.dp, c.outline.copy(alpha = 0.7f), RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(34.dp)) {
                val barH = size.height * 0.2f
                val gap = size.height * 0.46f
                val top = (size.height - (barH + gap)) / 2f
                listOf(top, top + gap).forEach { y ->
                    drawRoundRect(c.accent, Offset(0f, y), Size(size.width, barH), CornerRadius(barH / 2f))
                }
            }
        }
        Text(
            text = "No calculations yet",
            modifier = Modifier.padding(top = 20.dp),
            color = c.text,
            fontFamily = ArnoldFonts.Display,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
        )
        Text(
            text = "Results appear here after you press =.\nTap one to copy it, reuse it or edit it.",
            modifier = Modifier.padding(top = 8.dp),
            color = c.textSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
        )
    }
}
