package dev.arnoldsemase.calculator.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.arnoldsemase.calculator.ui.components.ArnoldSnackbar
import dev.arnoldsemase.calculator.ui.components.SubScreenBar
import dev.arnoldsemase.calculator.ui.theme.Arnold
import dev.arnoldsemase.calculator.ui.theme.ArnoldFonts
import kotlinx.coroutines.launch

/** One entry in the Tools hub. [available] flips to true as each tool screen ships. */
private data class ToolItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val glyph: String,
    val available: Boolean = false,
)

private data class ToolSection(val title: String, val items: List<ToolItem>)

private val SECTIONS = listOf(
    ToolSection(
        "Convert",
        listOf(
            ToolItem("units", "Units", "Length, mass, temperature, speed", "↔", available = true),
            ToolItem("currency", "Currency", "Live rates, cached offline", "$", available = true),
        ),
    ),
    ToolSection(
        "Calculate",
        listOf(
            ToolItem("percentage", "Percentage", "Of, change, tip, discount", "%", available = true),
            ToolItem("fractions", "Fractions", "Add, simplify, mixed numbers", "½", available = true),
            ToolItem("equations", "Equations", "Linear, quadratic, systems", "x=", available = true),
            ToolItem("dates", "Dates", "Difference, add days, age", "Δd", available = true),
            ToolItem("grapher", "Grapher", "Plot functions of x, zoom, trace", "ƒx", available = true),
        ),
    ),
)

/** Sits below the grid as a full-width card. */
private val SMART_PAD = ToolItem(
    "smartpad", "Smart Pad", "Type naturally, mix units, use variables", "≡=", available = true,
)

@Composable
fun ToolsScreen(onBack: () -> Unit, onOpenTool: (String) -> Unit) {
    val c = Arnold.colors
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val open: (ToolItem) -> Unit = { tool ->
        if (tool.available) {
            onOpenTool(tool.id)
        } else {
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar("${tool.title} is coming soon")
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
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SubScreenBar(title = "Tools", onBack = onBack)
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SECTIONS.forEach { section ->
                    SectionLabel(section.title)
                    section.items.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEach { tool -> ToolTile(tool, Modifier.weight(1f), onClick = { open(tool) }) }
                        }
                    }
                }
                ToolTile(SMART_PAD, Modifier.fillMaxWidth(), onClick = { open(SMART_PAD) })
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        color = Arnold.colors.textSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
    )
}

@Composable
private fun ToolTile(tool: ToolItem, modifier: Modifier, onClick: () -> Unit) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .defaultMinSize(minHeight = 112.dp)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outline.copy(alpha = 0.6f), shape)
            .clickable(
                role = Role.Button,
                onClickLabel = if (tool.available) "Open ${tool.title}" else "${tool.title}, coming soon",
                onClick = onClick,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.accentTint(0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tool.glyph,
                    color = c.accentText,
                    fontFamily = ArnoldFonts.Display,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            if (!tool.available) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = "Soon",
                        color = c.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = tool.title,
                color = c.text,
                fontFamily = ArnoldFonts.Display,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Text(
                text = tool.subtitle,
                color = c.textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}
