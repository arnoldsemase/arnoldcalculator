package dev.arnoldsemase.calculator.ui.calculator

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.arnoldsemase.calculator.ui.components.SegmentedChoice
import dev.arnoldsemase.calculator.ui.theme.Arnold
import dev.arnoldsemase.calculator.ui.theme.ArnoldFonts
import androidx.compose.foundation.layout.padding

/** Small pill showing the value held in memory. */
@Composable
private fun MemoryChip(value: String) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outline.copy(alpha = 0.7f), shape)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .semantics { contentDescription = "Memory holds $value" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(c.accent))
        Text(
            text = "M = $value",
            modifier = Modifier.padding(start = 6.dp),
            color = c.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            style = TextStyle(fontFeatureSettings = "tnum"),
            maxLines = 1,
        )
    }
}

/** Single-line text that right-aligns, keeps the newest characters in view and scrolls when too long. */
@Composable
private fun TrailingText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    LaunchedEffect(scroll) { snapshotFlow { scroll.maxValue }.collect { scroll.scrollTo(it) } }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            text = text,
            style = style,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.horizontalScroll(scroll),
        )
    }
}

@Composable
fun DisplayPanel(
    display: DisplayState,
    onCopy: () -> Unit,
    onToggleScientific: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val c = Arnold.colors

    val length = display.primary.length
    val target = when {
        display.isError -> 26f
        compact -> when { length <= 9 -> 44f; length <= 13 -> 36f; else -> 28f }
        length <= 7 -> 68f
        length <= 9 -> 58f
        length <= 11 -> 48f
        length <= 14 -> 40f
        length <= 17 -> 33f
        else -> 28f
    }
    val size by animateFloatAsState(target, tween(180), label = "resultSize")

    val shake = remember { Animatable(0f) }
    LaunchedEffect(display.errorTick) {
        if (display.errorTick > 0) {
            for (v in listOf(-10f, 10f, -6f, 6f, 0f)) shake.animateTo(v, tween(45))
        }
    }

    Column(modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Bottom) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            SegmentedChoice(
                options = listOf(false to "Standard", true to "Scientific"),
                selected = display.scientific,
                onSelect = { if (it != display.scientific) onToggleScientific() },
                compact = true,
            )
            if (display.memory != null) {
                Spacer(Modifier.weight(1f))
                MemoryChip(display.memory)
            }
        }
        Spacer(Modifier.weight(1f))
        TrailingText(
            text = display.secondary,
            style = TextStyle(
                color = c.textSecondary,
                fontFamily = ArnoldFonts.Body,
                fontSize = if (compact) 15.sp else 19.sp,
                fontFeatureSettings = "tnum",
            ),
            modifier = Modifier.heightIn(min = if (compact) 20.dp else 28.dp),
        )
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
                .then(
                    if (display.copyValue != null) {
                        Modifier.clickable(onClickLabel = "Copy result", onClick = onCopy)
                    } else {
                        Modifier
                    }
                )
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            TrailingText(
                text = display.primary,
                style = TextStyle(
                    color = if (display.isError) c.accentText else c.text,
                    fontFamily = ArnoldFonts.Display,
                    fontWeight = if (display.isError) FontWeight.Medium else FontWeight.Light,
                    fontSize = size.sp,
                    letterSpacing = (-0.5).sp,
                    fontFeatureSettings = "tnum",
                ),
            )
        }
    }
}
