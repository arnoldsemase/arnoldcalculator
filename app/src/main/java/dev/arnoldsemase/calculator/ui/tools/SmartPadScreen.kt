package dev.arnoldsemase.calculator.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.arnoldsemase.calculator.engine.NumberFormatSettings
import dev.arnoldsemase.calculator.engine.NumberFormatter
import dev.arnoldsemase.calculator.engine.SmartPad
import dev.arnoldsemase.calculator.ui.components.SubScreenBar
import dev.arnoldsemase.calculator.ui.settings.SettingsViewModel
import dev.arnoldsemase.calculator.ui.theme.Arnold
import dev.arnoldsemase.calculator.ui.theme.ArnoldFonts
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val PLACEHOLDER = "rent = 8500\n3 coffees at 45 + 15% tip\n20% of rent\n5 km in miles\ntotal"

/** Editor and results share one font size and line box so every result sits on its line's baseline. */
private val PadLineHeight = 26.sp
private val PadLineStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

@Composable
fun SmartPadScreen(
    viewModel: SmartPadViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val c = Arnold.colors
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    // Results are narrow, so "automatic" decimals stop at 6; an explicit setting is respected.
    val formatter = remember(settings.numberFormat) {
        val nf = settings.numberFormat
        NumberFormatter(if (nf.maxDecimals == NumberFormatSettings.AUTO) nf.copy(maxDecimals = 6) else nf)
    }

    LaunchedEffect(Unit) { viewModel.loadRates() }

    val text = viewModel.text
    val rates = viewModel.rates
    // Plain deterministic function of (text, rates): re-run on every keystroke.
    val results = remember(text, rates) { SmartPad.evaluate(text, rates) }

    val textStyle = TextStyle(
        fontFamily = ArnoldFonts.Body,
        fontSize = 18.sp,
        lineHeight = PadLineHeight,
        lineHeightStyle = PadLineStyle,
    )
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current
    val shape = RoundedCornerShape(22.dp)

    Scaffold(
        containerColor = c.background,
        contentWindowInsets = WindowInsets.systemBars,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SubScreenBar(title = "Smart Pad", onBack = onBack)
            BoxWithConstraints(
                Modifier
                    .widthIn(max = 640.dp)
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
                    .clip(shape)
                    .background(c.surface)
                    .border(1.dp, c.outline.copy(alpha = 0.6f), shape),
            ) {
                val minHeight = maxHeight
                Row(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = viewModel::onTextChange,
                        textStyle = textStyle.copy(color = c.text),
                        cursorBrush = SolidColor(c.accent),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = KeyboardType.Text,
                        ),
                        onTextLayout = { layout = it },
                        modifier = Modifier
                            .weight(1.5f)
                            .defaultMinSize(minHeight = minHeight - 28.dp)
                            .semantics { contentDescription = "Smart Pad editor" },
                        decorationBox = { inner ->
                            Box {
                                if (text.isEmpty()) {
                                    Text(
                                        PLACEHOLDER,
                                        style = textStyle,
                                        color = c.textSecondary.copy(alpha = 0.5f),
                                    )
                                }
                                inner()
                            }
                        },
                    )
                    Spacer(Modifier.width(12.dp))
                    // Each result is placed at the top of its line's first visual row, so wrapped lines stay aligned.
                    val starts = remember(text) { lineStarts(text) }
                    Box(Modifier.weight(1f)) {
                        val l = layout
                        if (l != null) {
                            results.forEachIndexed { i, value ->
                                if (value == null || i >= starts.size) return@forEachIndexed
                                val offset = starts[i].coerceIn(0, l.layoutInput.text.length)
                                val top = l.getLineTop(l.getLineForOffset(offset))
                                Text(
                                    text = SmartPad.format(value, formatter),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = with(density) { top.toDp() }),
                                    style = textStyle.copy(fontFamily = ArnoldFonts.Display, fontWeight = FontWeight.SemiBold),
                                    color = c.accentText,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Character offset where each '\n'-separated line starts. */
private fun lineStarts(text: String): List<Int> {
    val out = ArrayList<Int>()
    var start = 0
    out.add(0)
    for (i in text.indices) {
        if (text[i] == '\n') {
            start = i + 1
            out.add(start)
        }
    }
    return out
}
