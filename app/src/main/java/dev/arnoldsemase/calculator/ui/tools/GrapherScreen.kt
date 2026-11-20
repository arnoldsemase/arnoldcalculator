package dev.arnoldsemase.calculator.ui.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.arnoldsemase.calculator.engine.AngleMode
import dev.arnoldsemase.calculator.engine.GraphFn
import dev.arnoldsemase.calculator.engine.GraphFunction
import dev.arnoldsemase.calculator.ui.components.SegmentedChoice
import dev.arnoldsemase.calculator.ui.components.SubScreenBar
import dev.arnoldsemase.calculator.ui.components.rememberHaptics
import dev.arnoldsemase.calculator.ui.settings.SettingsViewModel
import dev.arnoldsemase.calculator.ui.theme.Arnold
import dev.arnoldsemase.calculator.ui.theme.ArnoldFonts
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private val CURVE_COLORS = listOf(Color(0xFFFF4C60), Color(0xFF4CC9F0), Color(0xFFFFC857))
private const val MAX_FUNCTIONS = 3
private const val MIN_SCALE = 2f
private const val MAX_SCALE = 20000f
private const val DEFAULT_SCALE = 48f

private val INSERT_KEYS = listOf(
    "x", "^", "(", ")", "π", "e", "√(", "abs(", "sin(", "cos(", "tan(", "ln(", "log(", "exp(",
)

/** 1, 2 or 5 × 10^n that is at least [raw]. */
private fun niceStep(raw: Double): Double {
    val exp = floor(log10(raw))
    val base = 10.0.pow(exp)
    val f = raw / base
    val nice = when {
        f <= 1.0 -> 1.0
        f <= 2.0 -> 2.0
        f <= 5.0 -> 5.0
        else -> 10.0
    }
    return nice * base
}

private fun fmt(v: Double): String {
    if (v.isNaN() || v.isInfinite()) return "—"
    if (abs(v) < 1e-9) return "0"
    if (abs(v) >= 1e9 || abs(v) < 1e-4) return String.format("%.3e", v)
    return BigDecimal(v).round(MathContext(6)).stripTrailingZeros().toPlainString()
}

@Composable
fun GrapherScreen(settingsViewModel: SettingsViewModel, onBack: () -> Unit) {
    val c = Arnold.colors
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(settings.haptics)

    var functions by rememberSaveable { mutableStateOf(listOf("sin(x)", "")) }
    var active by rememberSaveable { mutableStateOf(0) }
    var radians by rememberSaveable { mutableStateOf(true) }
    // World coordinates at the centre of the canvas, and pixels per unit.
    var centerX by rememberSaveable { mutableStateOf(0f) }
    var centerY by rememberSaveable { mutableStateOf(0f) }
    var scale by rememberSaveable { mutableStateOf(DEFAULT_SCALE) }
    var traceX by rememberSaveable { mutableStateOf<Float?>(null) }

    val angle = if (radians) AngleMode.RAD else AngleMode.DEG
    val compiled = functions.map { text -> if (text.isBlank()) null else GraphFunction.compile(text, angle) }

    Scaffold(
        containerColor = c.background,
        contentWindowInsets = WindowInsets.systemBars,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SubScreenBar(title = "Grapher", onBack = onBack)
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // ---- Function inputs
                functions.forEachIndexed { index, text ->
                    val ok = text.isBlank() || compiled[index] != null
                    FunctionRow(
                        index = index,
                        text = text,
                        valid = ok,
                        selected = index == active,
                        canRemove = functions.size > 1,
                        onFocus = { active = index },
                        onChange = { new ->
                            functions = functions.toMutableList().also { it[index] = new }
                        },
                        onRemove = {
                            haptics.tick()
                            functions = functions.toMutableList().also { it.removeAt(index) }
                            active = active.coerceIn(0, functions.size - 1)
                        },
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (functions.size < MAX_FUNCTIONS) {
                        Chip("+ Function", wide = true) {
                            haptics.tick()
                            functions = functions + ""
                            active = functions.size - 1
                        }
                    }
                    Box(Modifier.weight(1f))
                    SegmentedChoice(
                        options = listOf(true to "RAD", false to "DEG"),
                        selected = radians,
                        onSelect = { haptics.tick(); radians = it },
                        compact = true,
                    )
                }

                // ---- Quick insert keys
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    INSERT_KEYS.forEach { key ->
                        Chip(key) {
                            haptics.tick()
                            functions = functions.toMutableList().also { it[active] = it[active] + key }
                        }
                    }
                    Chip("⌫") {
                        haptics.tick()
                        functions = functions.toMutableList().also { it[active] = it[active].dropLast(1) }
                    }
                }

                // ---- Graph
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 12.dp),
                ) {
                    GraphCanvas(
                        functions = compiled,
                        centerX = centerX,
                        centerY = centerY,
                        scale = scale,
                        traceX = traceX,
                        onView = { cx, cy, s -> centerX = cx; centerY = cy; scale = s },
                        onTrace = { traceX = it },
                    )
                    Column(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        RoundButton("+", "Zoom in") {
                            haptics.tick()
                            scale = (scale * 1.4f).coerceIn(MIN_SCALE, MAX_SCALE)
                        }
                        RoundButton("−", "Zoom out") {
                            haptics.tick()
                            scale = (scale / 1.4f).coerceIn(MIN_SCALE, MAX_SCALE)
                        }
                        RoundButton("⟲", "Reset view") {
                            haptics.confirm()
                            centerX = 0f; centerY = 0f; scale = DEFAULT_SCALE; traceX = null
                        }
                    }
                    // Trace readout
                    val tx = traceX
                    if (tx != null) {
                        Column(
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.surfaceHigh)
                                .border(1.dp, c.outline, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text("x = ${fmt(tx.toDouble())}", color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            compiled.forEachIndexed { i, fn ->
                                if (fn != null) {
                                    Text(
                                        "y${i + 1} = ${fmt(fn.at(tx.toDouble()))}",
                                        color = CURVE_COLORS[i % CURVE_COLORS.size],
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FunctionRow(
    index: Int,
    text: String,
    valid: Boolean,
    selected: Boolean,
    canRemove: Boolean,
    onFocus: () -> Unit,
    onChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(14.dp)
    val color = CURVE_COLORS[index % CURVE_COLORS.size]
    val borderColor = when {
        !valid -> Color(0xFFE5484D)
        selected -> color
        else -> c.outline.copy(alpha = 0.7f)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text("y =", color = c.textSecondary, fontSize = 16.sp, fontFamily = ArnoldFonts.Display)
        BasicTextField(
            value = text,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            cursorBrush = SolidColor(c.accent),
            textStyle = TextStyle(
                color = c.text,
                fontFamily = ArnoldFonts.Display,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            ),
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "Function ${index + 1}" }
                .onFocusChanged { if (it.isFocused) onFocus() },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) Text("type a function of x", color = c.textSecondary, fontSize = 16.sp)
                    inner()
                }
            },
        )
        if (canRemove) {
            Text(
                "✕",
                color = c.textSecondary,
                fontSize = 16.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Remove function ${index + 1}", onClick = onRemove)
                    .padding(6.dp),
            )
        }
    }
}

@Composable
private fun Chip(label: String, wide: Boolean = false, onClick: () -> Unit) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (wide) c.accentTint(0.2f) else c.surface)
            .border(1.dp, c.outline.copy(alpha = 0.7f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (wide) c.accentText else c.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = ArnoldFonts.Display,
            maxLines = 1,
        )
    }
}

@Composable
private fun RoundButton(label: String, description: String, onClick: () -> Unit) {
    val c = Arnold.colors
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(c.surfaceHigh.copy(alpha = 0.92f))
            .border(1.dp, c.outline, CircleShape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GraphCanvas(
    functions: List<GraphFn?>,
    centerX: Float,
    centerY: Float,
    scale: Float,
    traceX: Float?,
    onView: (Float, Float, Float) -> Unit,
    onTrace: (Float?) -> Unit,
) {
    val c = Arnold.colors
    val measurer = rememberTextMeasurer()
    var size by remember { mutableStateOf(IntSize.Zero) }
    val shape = RoundedCornerShape(22.dp)
    // Gesture blocks key on Unit (re-keying mid-drag would cancel the gesture) and read live values here.
    val cx by rememberUpdatedState(centerX)
    val cy by rememberUpdatedState(centerY)
    val sc by rememberUpdatedState(scale)
    val onViewNow by rememberUpdatedState(onView)
    val onTraceNow by rememberUpdatedState(onTrace)

    val gridColor = c.outline.copy(alpha = 0.45f)
    val axisColor = c.textSecondary.copy(alpha = 0.8f)
    val labelStyle = TextStyle(color = c.textSecondary, fontSize = 10.sp)

    Canvas(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outline.copy(alpha = 0.6f), shape)
            .onSizeChanged { size = it }
            .semantics { contentDescription = "Graph. Drag to pan, pinch to zoom, tap to trace." }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val newScale = (sc * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                    // Keep the world point under the fingers fixed while zooming.
                    val wx = cx + (centroid.x - w / 2f) / sc
                    val wy = cy - (centroid.y - h / 2f) / sc
                    val ncx = wx - (centroid.x + pan.x - w / 2f) / newScale
                    val ncy = wy + (centroid.y + pan.y - h / 2f) / newScale
                    onViewNow(ncx, ncy, newScale)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    val w = size.width.toFloat()
                    onTraceNow(cx + (pos.x - w / 2f) / sc)
                }
            },
    ) {
        val w = this.size.width
        val h = this.size.height
        if (w <= 0f || h <= 0f) return@Canvas

        fun sx(wx: Double) = (w / 2.0 + (wx - centerX) * scale).toFloat()
        fun sy(wy: Double) = (h / 2.0 - (wy - centerY) * scale).toFloat()

        val left = centerX - (w / 2f) / scale
        val right = centerX + (w / 2f) / scale
        val bottom = centerY - (h / 2f) / scale
        val top = centerY + (h / 2f) / scale

        // ---- Grid
        val step = niceStep(80.0 / scale)
        var gx = floor(left / step) * step
        while (gx <= right) {
            val x = sx(gx)
            drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
            gx += step
        }
        var gy = floor(bottom / step) * step
        while (gy <= top) {
            val y = sy(gy)
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            gy += step
        }

        // ---- Axes
        val axisY = sy(0.0).coerceIn(0f, h)
        val axisX = sx(0.0).coerceIn(0f, w)
        drawLine(axisColor, Offset(0f, axisY), Offset(w, axisY), strokeWidth = 2f)
        drawLine(axisColor, Offset(axisX, 0f), Offset(axisX, h), strokeWidth = 2f)

        // ---- Axis labels
        var lx = floor(left / step) * step
        while (lx <= right) {
            if (abs(lx) > step / 2) {
                val label = measurer.measure(fmt(lx), labelStyle)
                val px = (sx(lx) - label.size.width / 2f).coerceIn(2f, w - label.size.width - 2f)
                val py = (axisY + 3f).coerceAtMost(h - label.size.height - 2f)
                drawText(label, topLeft = Offset(px, py))
            }
            lx += step
        }
        var ly = floor(bottom / step) * step
        while (ly <= top) {
            if (abs(ly) > step / 2) {
                val label = measurer.measure(fmt(ly), labelStyle)
                val px = (axisX + 4f).coerceAtMost(w - label.size.width - 2f)
                val py = (sy(ly) - label.size.height / 2f).coerceIn(2f, h - label.size.height - 2f)
                drawText(label, topLeft = Offset(px, py))
            }
            ly += step
        }

        // ---- Curves
        val strokeW = 2.5.dp.toPx()
        functions.forEachIndexed { i, fn ->
            if (fn == null) return@forEachIndexed
            val color = CURVE_COLORS[i % CURVE_COLORS.size]
            val path = Path()
            var penDown = false
            var prevY = 0f
            val stepPx = 1f
            var px = 0f
            while (px <= w) {
                val wx = centerX + (px - w / 2f) / scale.toDouble()
                val v = fn.at(wx)
                if (v.isNaN() || v.isInfinite() || abs(v) > 1e7) {
                    penDown = false
                } else {
                    val py = sy(v)
                    // A jump taller than the view is an asymptote or a gap: lift the pen.
                    if (penDown && abs(py - prevY) < h * 1.5f) {
                        path.lineTo(px, py)
                    } else {
                        path.moveTo(px, py)
                    }
                    penDown = true
                    prevY = py
                }
                px += stepPx
            }
            drawPath(path, color, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // ---- Trace marker
        if (traceX != null) {
            val x = sx(traceX.toDouble())
            drawLine(c.textSecondary.copy(alpha = 0.6f), Offset(x, 0f), Offset(x, h), strokeWidth = 1.5f)
            functions.forEachIndexed { i, fn ->
                if (fn == null) return@forEachIndexed
                val v = fn.at(traceX.toDouble())
                if (!v.isNaN() && !v.isInfinite()) {
                    val y = sy(v)
                    if (y in 0f..h) {
                        val color = CURVE_COLORS[i % CURVE_COLORS.size]
                        drawCircle(c.surface, radius = 7.dp.toPx(), center = Offset(x, y))
                        drawCircle(color, radius = 5.dp.toPx(), center = Offset(x, y))
                    }
                }
            }
        }
    }
}
