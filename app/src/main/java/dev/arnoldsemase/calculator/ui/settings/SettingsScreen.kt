package dev.arnoldsemase.calculator.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.selection.toggleable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.arnoldsemase.calculator.data.ThemeMode
import dev.arnoldsemase.calculator.engine.DecimalMark
import dev.arnoldsemase.calculator.engine.GroupingStyle
import dev.arnoldsemase.calculator.engine.NumberFormatSettings
import dev.arnoldsemase.calculator.engine.NumberFormatter
import dev.arnoldsemase.calculator.ui.components.ArnoldCard
import dev.arnoldsemase.calculator.ui.components.BrandMark
import dev.arnoldsemase.calculator.ui.components.SegmentedChoice
import dev.arnoldsemase.calculator.ui.components.SubScreenBar
import dev.arnoldsemase.calculator.ui.theme.Arnold
import dev.arnoldsemase.calculator.ui.theme.ArnoldFonts

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val c = Arnold.colors
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0.0"
    }

    Scaffold(containerColor = c.background, contentWindowInsets = WindowInsets.systemBars) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SubScreenBar(title = "Settings", onBack = onBack)
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SectionTitle("Appearance")
                ArnoldCard {
                    SegmentedChoice(
                        options = listOf(ThemeMode.DARK to "Dark", ThemeMode.LIGHT to "Light", ThemeMode.SYSTEM to "System"),
                        selected = settings.theme,
                        onSelect = viewModel::setTheme,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Hint("Dark is the Arnold default. System follows your phone.")
                }

                SectionTitle("Feedback")
                ArnoldCard {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = settings.haptics,
                                role = Role.Switch,
                                onValueChange = viewModel::setHaptics,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Haptic feedback", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("A light tap on every key press", color = c.textSecondary, fontSize = 13.sp)
                        }
                        Switch(
                            checked = settings.haptics,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = c.onAccent,
                                checkedTrackColor = c.accent,
                                checkedBorderColor = c.accent,
                                uncheckedThumbColor = c.textSecondary,
                                uncheckedTrackColor = c.surfaceHigh,
                                uncheckedBorderColor = c.outline,
                            ),
                        )
                    }
                }

                SectionTitle("Number format")
                ArnoldCard {
                    FieldLabel("Thousands separator")
                    SegmentedChoice(
                        options = GroupingStyle.entries.map { it to it.label },
                        selected = settings.numberFormat.grouping,
                        onSelect = viewModel::setGrouping,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FieldLabel("Decimal mark", top = 16.dp)
                    SegmentedChoice(
                        options = DecimalMark.entries.map { it to it.label },
                        selected = settings.numberFormat.decimalMark,
                        onSelect = viewModel::setDecimalMark,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FieldLabel("Maximum decimal places", top = 16.dp)
                    SegmentedChoice(
                        options = listOf(NumberFormatSettings.AUTO to "Auto", 2 to "2", 4 to "4", 6 to "6", 8 to "8"),
                        selected = settings.numberFormat.maxDecimals,
                        onSelect = viewModel::setMaxDecimals,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Preview", color = c.textSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(
                            text = NumberFormatter(settings.numberFormat).display("1234567.891234"),
                            color = c.text,
                            fontFamily = ArnoldFonts.Display,
                            fontWeight = FontWeight.Light,
                            fontSize = 24.sp,
                        )
                    }
                }

                SectionTitle("About")
                ArnoldCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(size = 52.dp)
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(
                                "Arnold Calculator",
                                color = c.text,
                                fontFamily = ArnoldFonts.Display,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            )
                            Text("Version $version", color = c.textSecondary, fontSize = 13.sp)
                        }
                    }
                    Text(
                        text = "Part of the Arnold OS Apps ecosystem. Everything runs on your device: no account, no ads, no tracking.",
                        modifier = Modifier.padding(top = 14.dp),
                        color = c.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    )
                    Text(
                        text = "arnoldsemase.dev",
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .clickable(role = Role.Button, onClickLabel = "Open website") {
                                uriHandler.openUri("https://arnoldsemase.dev")
                            }
                            .padding(vertical = 10.dp),
                        color = c.accentText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
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
private fun FieldLabel(text: String, top: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text = text,
        modifier = Modifier.padding(top = top, bottom = 8.dp),
        color = Arnold.colors.text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun Hint(text: String) {
    Text(text, modifier = Modifier.padding(top = 10.dp), color = Arnold.colors.textSecondary, fontSize = 13.sp)
}

