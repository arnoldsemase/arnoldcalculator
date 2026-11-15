@file:OptIn(ExperimentalFoundationApi::class)

package dev.arnoldsemase.calculator.ui.calculator

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.arnoldsemase.calculator.engine.AngleMode
import dev.arnoldsemase.calculator.ui.components.BackspaceGlyph
import dev.arnoldsemase.calculator.ui.components.Haptics
import dev.arnoldsemase.calculator.ui.theme.Arnold
import dev.arnoldsemase.calculator.ui.theme.ArnoldFonts

enum class KeyStyle { Number, Operator, Utility, Equals, Science, ScienceActive }

/** Scales key glyphs down in landscape, where keys are shorter. */
val LocalKeyScale = compositionLocalOf { 1f }

@Composable
fun CalcKey(
    modifier: Modifier,
    style: KeyStyle,
    description: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val c = Arnold.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "keyScale",
    )
    val overlay by animateFloatAsState(if (pressed) 0.12f else 0f, label = "keyOverlay")

    val isScience = style == KeyStyle.Science || style == KeyStyle.ScienceActive
    val shape = RoundedCornerShape(if (isScience) 16.dp else 22.dp)

    val brush: Brush = when (style) {
        KeyStyle.Number -> Brush.verticalGradient(listOf(lerp(c.surface, Color.White, if (c.isDark) 0.05f else 0f), c.surface))
        KeyStyle.Operator -> Brush.verticalGradient(listOf(c.accentTint(0.17f), c.accentTint(0.13f)))
        KeyStyle.Utility -> Brush.verticalGradient(listOf(c.surfaceHigh, c.surfaceHigh))
        KeyStyle.Equals -> Brush.linearGradient(listOf(c.accentSecondary, c.accent))
        KeyStyle.Science -> Brush.verticalGradient(listOf(c.surface, c.surface))
        KeyStyle.ScienceActive -> Brush.verticalGradient(listOf(c.accentTint(0.2f), c.accentTint(0.2f)))
    }
    val borderColor: Color = when (style) {
        KeyStyle.Equals -> Color.Transparent
        KeyStyle.Operator, KeyStyle.ScienceActive -> c.accent.copy(alpha = 0.28f)
        else -> c.outline.copy(alpha = 0.6f)
    }

    var keyModifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    if (style == KeyStyle.Equals) {
        keyModifier = keyModifier.shadow(
            elevation = 12.dp,
            shape = shape,
            ambientColor = c.accent.copy(alpha = 0.35f),
            spotColor = c.accent.copy(alpha = 0.55f),
        )
    }

    Box(
        keyModifier
            .clip(shape)
            .background(brush)
            .border(1.dp, borderColor, shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = description,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.matchParentSize().background(c.text.copy(alpha = overlay)))
        Box(Modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
fun KeyLabel(text: String, style: KeyStyle) {
    val c = Arnold.colors
    val k = LocalKeyScale.current
    val (size, weight, color) = when (style) {
        KeyStyle.Number -> Triple(28.sp, FontWeight.Medium, c.text)
        KeyStyle.Operator -> Triple(30.sp, FontWeight.Medium, c.accentText)
        KeyStyle.Utility -> Triple(22.sp, FontWeight.Medium, c.text)
        KeyStyle.Equals -> Triple(32.sp, FontWeight.Bold, c.onAccent)
        KeyStyle.Science -> Triple(15.sp, FontWeight.Medium, c.text)
        KeyStyle.ScienceActive -> Triple(14.sp, FontWeight.Bold, c.accentText)
    }
    Text(
        text = text,
        color = color,
        fontFamily = ArnoldFonts.Display,
        fontWeight = weight,
        fontSize = size * k,
        maxLines = 1,
        softWrap = false,
    )
}

@Composable
private fun ColumnScope.KeyRow(gap: Dp, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .weight(1f),
        horizontalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

@Composable
private fun RowScope.Key(
    label: String,
    style: KeyStyle,
    description: String,
    haptics: Haptics,
    strong: Boolean = false,
    onClick: () -> Unit,
) {
    CalcKey(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        style = style,
        description = description,
        onClick = {
            if (strong) haptics.confirm() else haptics.tick()
            onClick()
        },
    ) { KeyLabel(label, style) }
}

/** Standard 4 × 5 keypad. Rows share the height they are given. */
@Composable
fun StandardPad(
    input: CalculatorInput,
    haptics: Haptics,
    decimalMark: Char,
    modifier: Modifier = Modifier,
    gap: Dp = 10.dp,
) {
    val c = Arnold.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        KeyRow(gap) {
            Key("AC", KeyStyle.Utility, "All clear", haptics) { input.clear() }
            Key("( )", KeyStyle.Utility, "Parentheses", haptics) { input.parenthesis() }
            Key("%", KeyStyle.Utility, "Percent", haptics) { input.percent() }
            Key("÷", KeyStyle.Operator, "Divide", haptics) { input.operation('÷') }
        }
        KeyRow(gap) {
            Key("7", KeyStyle.Number, "7", haptics) { input.digit('7') }
            Key("8", KeyStyle.Number, "8", haptics) { input.digit('8') }
            Key("9", KeyStyle.Number, "9", haptics) { input.digit('9') }
            Key("×", KeyStyle.Operator, "Multiply", haptics) { input.operation('×') }
        }
        KeyRow(gap) {
            Key("4", KeyStyle.Number, "4", haptics) { input.digit('4') }
            Key("5", KeyStyle.Number, "5", haptics) { input.digit('5') }
            Key("6", KeyStyle.Number, "6", haptics) { input.digit('6') }
            Key("−", KeyStyle.Operator, "Minus", haptics) { input.operation('−') }
        }
        KeyRow(gap) {
            Key("1", KeyStyle.Number, "1", haptics) { input.digit('1') }
            Key("2", KeyStyle.Number, "2", haptics) { input.digit('2') }
            Key("3", KeyStyle.Number, "3", haptics) { input.digit('3') }
            Key("+", KeyStyle.Operator, "Plus", haptics) { input.operation('+') }
        }
        KeyRow(gap) {
            Key("0", KeyStyle.Number, "0", haptics) { input.digit('0') }
            Key(decimalMark.toString(), KeyStyle.Number, "Decimal point", haptics) { input.decimal() }
            CalcKey(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                style = KeyStyle.Utility,
                description = "Backspace. Long press to clear everything",
                onClick = { haptics.tick(); input.backspace() },
                onLongClick = { haptics.confirm(); input.clear() },
            ) { BackspaceGlyph(c.text) }
            Key("=", KeyStyle.Equals, "Equals", haptics, strong = true) { input.evaluate() }
        }
    }
}

/** Scientific 4 × 4 panel shown above the standard keypad. */
@Composable
fun SciencePad(
    input: CalculatorInput,
    haptics: Haptics,
    angle: AngleMode,
    modifier: Modifier = Modifier,
    gap: Dp = 8.dp,
) {
    val angleName = if (angle == AngleMode.DEG) "degrees" else "radians"
    val other = if (angle == AngleMode.DEG) "radians" else "degrees"
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        KeyRow(gap) {
            Key(angle.name, KeyStyle.ScienceActive, "Angle unit: $angleName. Tap for $other", haptics) { input.toggleAngle() }
            Key("sin", KeyStyle.Science, "Sine", haptics) { input.function("sin") }
            Key("cos", KeyStyle.Science, "Cosine", haptics) { input.function("cos") }
            Key("tan", KeyStyle.Science, "Tangent", haptics) { input.function("tan") }
        }
        KeyRow(gap) {
            Key("ln", KeyStyle.Science, "Natural logarithm", haptics) { input.function("ln") }
            Key("log", KeyStyle.Science, "Logarithm base 10", haptics) { input.function("log") }
            Key("√", KeyStyle.Science, "Square root", haptics) { input.function("√") }
            Key("xʸ", KeyStyle.Science, "Power", haptics) { input.operation('^') }
        }
        KeyRow(gap) {
            Key("π", KeyStyle.Science, "Pi", haptics) { input.constant("π") }
            Key("e", KeyStyle.Science, "Euler's number", haptics) { input.constant("e") }
            Key("x²", KeyStyle.Science, "Square", haptics) { input.suffix("^2") }
            Key("n!", KeyStyle.Science, "Factorial", haptics) { input.factorial() }
        }
        KeyRow(gap) {
            Key("sin⁻¹", KeyStyle.Science, "Inverse sine", haptics) { input.function("asin") }
            Key("cos⁻¹", KeyStyle.Science, "Inverse cosine", haptics) { input.function("acos") }
            Key("tan⁻¹", KeyStyle.Science, "Inverse tangent", haptics) { input.function("atan") }
            Key("1/x", KeyStyle.Science, "Reciprocal", haptics) { input.suffix("^(−1)") }
        }
    }
}
