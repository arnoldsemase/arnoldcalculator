package dev.arnoldsemase.calculator.ui.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.arnoldsemase.calculator.ui.components.ArnoldIconButton
import dev.arnoldsemase.calculator.ui.components.ArnoldSnackbar
import dev.arnoldsemase.calculator.ui.components.BrandTitle
import dev.arnoldsemase.calculator.ui.components.HistoryGlyph
import dev.arnoldsemase.calculator.ui.components.rememberHaptics
import dev.arnoldsemase.calculator.ui.theme.Arnold
import kotlinx.coroutines.launch

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = Arnold.colors
    val display by viewModel.display.collectAsStateWithLifecycle()
    val hapticsOn by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(hapticsOn)
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(display.errorTick) {
        if (display.errorTick > 0) haptics.reject()
    }

    val copy: () -> Unit = {
        display.copyValue?.let { value ->
            clipboard.setText(AnnotatedString(value))
            haptics.confirm()
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar("Copied to clipboard")
            }
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
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandTitle()
                Spacer(Modifier.weight(1f))
                ArnoldIconButton(onClick = onOpenHistory, description = "History") {
                    HistoryGlyph(c.text, Modifier.size(22.dp))
                }
                ArnoldIconButton(onClick = onOpenSettings, description = "Settings") {
                    Icon(Icons.Rounded.Settings, contentDescription = null, tint = c.text, modifier = Modifier.size(21.dp))
                }
            }

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val landscape = maxWidth > maxHeight
                val gap = 10.dp
                val contentModifier = Modifier
                    .widthIn(max = if (landscape) 920.dp else 560.dp)
                    .align(Alignment.TopCenter)

                if (landscape) {
                    val keyH = ((maxHeight - 72.dp - gap * 5) / 5).coerceIn(30.dp, 58.dp)
                    val padH = keyH * 5 + gap * 4
                    CompositionLocalProvider(LocalKeyScale provides 0.72f) {
                        Column(contentModifier.fillMaxSize().padding(bottom = 10.dp)) {
                            DisplayPanel(
                                display = display,
                                onCopy = copy,
                                onToggleScientific = viewModel::toggleScientific,
                                modifier = Modifier.weight(1f).padding(bottom = 8.dp),
                                compact = true,
                            )
                            Row(Modifier.fillMaxWidth().height(padH), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                if (display.scientific) {
                                    SciencePad(viewModel, haptics, display.angle, Modifier.weight(1f).height(padH), gap = 8.dp)
                                }
                                StandardPad(viewModel, haptics, display.decimalMark, Modifier.weight(1f).height(padH), gap)
                            }
                        }
                    }
                } else {
                    val sciKey = if (maxHeight < 720.dp) 36.dp else 42.dp
                    val sciGap = 8.dp
                    val sciH = sciKey * 4 + sciGap * 3
                    val minDisplay = 176.dp
                    val available = maxHeight - minDisplay - 12.dp -
                        (if (display.scientific) sciH + gap else 0.dp)
                    val stdKeyTarget = ((available - gap * 4) / 5).coerceIn(44.dp, 78.dp)
                    val stdKey by animateDpAsState(stdKeyTarget, tween(260), label = "stdKey")
                    val stdH = stdKey * 5 + gap * 4

                    Column(contentModifier.fillMaxSize().padding(bottom = 12.dp)) {
                        DisplayPanel(
                            display = display,
                            onCopy = copy,
                            onToggleScientific = viewModel::toggleScientific,
                            modifier = Modifier.weight(1f).padding(bottom = 12.dp),
                        )
                        AnimatedVisibility(
                            visible = display.scientific,
                            enter = expandVertically(tween(260)) + fadeIn(tween(260)),
                            exit = shrinkVertically(tween(220)) + fadeOut(tween(160)),
                        ) {
                            Column {
                                CompositionLocalProvider(LocalKeyScale provides if (sciKey < 40.dp) 0.92f else 1f) {
                                    SciencePad(viewModel, haptics, display.angle, Modifier.fillMaxWidth().height(sciH), sciGap)
                                }
                                Spacer(Modifier.height(gap))
                            }
                        }
                        StandardPad(viewModel, haptics, display.decimalMark, Modifier.fillMaxWidth().height(stdH), gap)
                    }
                }
            }
        }
    }
}
