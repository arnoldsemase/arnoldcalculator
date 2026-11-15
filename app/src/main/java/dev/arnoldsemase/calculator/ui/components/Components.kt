package dev.arnoldsemase.calculator.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.arnoldsemase.calculator.ui.theme.Arnold
import dev.arnoldsemase.calculator.ui.theme.ArnoldFonts

/**
 * Arnold logo: a coral keycap carrying a chevron "A" over an "=" sign.
 * Geometry is on a 60-unit grid and matches ic_launcher_foreground / branding/arnold-logo.svg.
 */
@Composable
fun ArnoldLogo(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 60f
        val corner = CornerRadius(18f * k)
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color(0xFFFF7384), Color(0xFFFF4C60), Color(0xFFE63049)),
                start = Offset(6f * k, 0f),
                end = Offset(56f * k, 62f * k),
            ),
            cornerRadius = corner,
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                startY = 0f,
                endY = 36f * k,
            ),
            cornerRadius = corner,
        )
        val ink = Color(0xFF0F0F0F)
        val legs = Path().apply {
            moveTo(14.5f * k, 50f * k)
            lineTo(30f * k, 10f * k)
            lineTo(45.5f * k, 50f * k)
        }
        drawPath(legs, ink, style = Stroke(6.5f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(ink, Offset(21.1f * k, 33f * k), Offset(38.9f * k, 33f * k), 4.8f * k, StrokeCap.Round)
        drawLine(ink, Offset(17.6f * k, 42f * k), Offset(42.4f * k, 42f * k), 4.8f * k, StrokeCap.Round)
    }
}

@Composable
fun BrandMark(size: Dp = 30.dp) = ArnoldLogo(size)

@Composable
fun BrandTitle() {
    val c = Arnold.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        BrandMark()
        Text(
            text = "Arnold",
            modifier = Modifier.padding(start = 10.dp),
            color = c.text,
            fontFamily = ArnoldFonts.Display,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        Text(
            text = " Calculator",
            color = c.textSecondary,
            fontFamily = ArnoldFonts.Display,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
        )
    }
}

@Composable
fun ArnoldIconButton(onClick: () -> Unit, description: String, content: @Composable () -> Unit) {
    val c = Arnold.colors
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(c.surface)
                .border(1.dp, c.outline.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

/** Top bar used by History and Settings. */
@Composable
fun SubScreenBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    val c = Arnold.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArnoldIconButton(onClick = onBack, description = "Back") {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                tint = c.text,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            color = c.text,
            fontFamily = ArnoldFonts.Display,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
        )
        actions()
    }
}

@Composable
fun HistoryGlyph(color: Color, modifier: Modifier = Modifier.size(22.dp)) {
    Canvas(modifier) {
        val s = size.minDimension
        val w = s * 0.09f
        drawCircle(color, radius = s * 0.42f, style = Stroke(w, cap = StrokeCap.Round))
        drawLine(color, Offset(s * 0.5f, s * 0.28f), Offset(s * 0.5f, s * 0.5f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.65f, s * 0.6f), w, StrokeCap.Round)
    }
}

@Composable
fun BackspaceGlyph(color: Color, modifier: Modifier = Modifier.size(26.dp)) {
    Canvas(modifier) {
        val k = size.minDimension / 24f
        val w = 1.9f * k
        val outline = Path().apply {
            moveTo(9f * k, 5f * k)
            lineTo(21f * k, 5f * k)
            lineTo(21f * k, 19f * k)
            lineTo(9f * k, 19f * k)
            lineTo(2.5f * k, 12f * k)
            close()
        }
        drawPath(outline, color, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(12f * k, 9.5f * k), Offset(17f * k, 14.5f * k), w, StrokeCap.Round)
        drawLine(color, Offset(17f * k, 9.5f * k), Offset(12f * k, 14.5f * k), w, StrokeCap.Round)
    }
}

@Composable
fun ArnoldCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outline.copy(alpha = 0.6f), shape)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun <T> SegmentedChoice(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val c = Arnold.colors
    val outerShape = RoundedCornerShape(if (compact) 14.dp else 16.dp)
    Row(
        modifier
            .clip(outerShape)
            .background(c.background)
            .border(1.dp, c.outline.copy(alpha = 0.7f), outerShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val bg by animateColorAsState(if (isSelected) c.accentTint(0.2f) else Color.Transparent, label = "segBg")
            val fg by animateColorAsState(if (isSelected) c.accentText else c.textSecondary, label = "segFg")
            val itemModifier = if (compact) {
                Modifier.padding(0.dp)
            } else {
                Modifier.weight(1f)
            }
            Box(
                itemModifier
                    .clip(RoundedCornerShape(if (compact) 10.dp else 12.dp))
                    .background(bg)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(value) })
                    .padding(horizontal = if (compact) 14.dp else 8.dp, vertical = if (compact) 8.dp else 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = fg,
                    fontSize = if (compact) 13.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun ArnoldSnackbar(data: SnackbarData) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(c.surfaceHigh)
            .border(1.dp, c.outline, shape)
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = data.visuals.message,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            color = c.text,
            fontSize = 14.sp,
        )
        data.visuals.actionLabel?.let { label ->
            TextButton(onClick = { data.performAction() }) {
                Text(label, color = c.accentText, fontWeight = FontWeight.Bold)
            }
        }
    }
}
