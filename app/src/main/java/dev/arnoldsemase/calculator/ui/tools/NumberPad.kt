package dev.arnoldsemase.calculator.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.arnoldsemase.calculator.ui.calculator.CalcKey
import dev.arnoldsemase.calculator.ui.calculator.KeyLabel
import dev.arnoldsemase.calculator.ui.calculator.KeyStyle
import dev.arnoldsemase.calculator.ui.components.BackspaceGlyph
import dev.arnoldsemase.calculator.ui.components.Haptics
import dev.arnoldsemase.calculator.ui.theme.Arnold
import dev.arnoldsemase.calculator.ui.theme.ArnoldFonts

// Shared by the Units and Currency tools. Input is kept as plain text ("-12.5"); formatting is display-only.

private const val MAX_DIGITS = 15

// ---- Typing helpers: input is kept as plain text ("-12.5"); formatting is display-only -----------

internal fun typeDigits(raw: String, digits: String): String {
    var out = raw
    for (d in digits) {
        val count = out.count { it in '0'..'9' }
        out = when {
            count >= MAX_DIGITS -> out
            out == "0" -> d.toString()
            out == "-0" -> "-$d"
            else -> out + d
        }
    }
    return out
}

internal fun typeDecimal(raw: String): String = when {
    '.' in raw -> raw
    raw.isEmpty() || raw == "-" -> raw + "0."
    else -> "$raw."
}

internal fun typeBackspace(raw: String): String = raw.dropLast(1).let { if (it == "-") "" else it }

internal fun typeNegate(raw: String): String = if (raw.startsWith("-")) raw.drop(1) else "-$raw"

@Composable
private fun RowScope.PadKey(
    label: String,
    style: KeyStyle,
    description: String,
    haptics: Haptics,
    onClick: () -> Unit,
) {
    CalcKey(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        style = style,
        description = description,
        onClick = {
            haptics.tick()
            onClick()
        },
    ) { KeyLabel(label, style) }
}

@Composable
fun NumberPad(
    haptics: Haptics,
    decimalMark: Char,
    onDigits: (String) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onNegate: () -> Unit,
    onCopy: () -> Unit,
    /** The key under 3 on the right. Defaults to the sign toggle; tools can repurpose it (e.g. "Next"). */
    extraLabel: String = "±",
    extraDescription: String = "Change sign",
) {
    val c = Arnold.colors
    val gap = 10.dp
    val rowModifier = Modifier
        .fillMaxWidth()
        .height(60.dp)
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(rowModifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
            PadKey("7", KeyStyle.Number, "7", haptics) { onDigits("7") }
            PadKey("8", KeyStyle.Number, "8", haptics) { onDigits("8") }
            PadKey("9", KeyStyle.Number, "9", haptics) { onDigits("9") }
            CalcKey(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                style = KeyStyle.Utility,
                description = "Backspace. Long press to clear",
                onClick = { haptics.tick(); onBackspace() },
                onLongClick = { haptics.confirm(); onClear() },
            ) { BackspaceGlyph(c.text) }
        }
        Row(rowModifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
            PadKey("4", KeyStyle.Number, "4", haptics) { onDigits("4") }
            PadKey("5", KeyStyle.Number, "5", haptics) { onDigits("5") }
            PadKey("6", KeyStyle.Number, "6", haptics) { onDigits("6") }
            PadKey("AC", KeyStyle.Utility, "Clear", haptics) { onClear() }
        }
        Row(rowModifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
            PadKey("1", KeyStyle.Number, "1", haptics) { onDigits("1") }
            PadKey("2", KeyStyle.Number, "2", haptics) { onDigits("2") }
            PadKey("3", KeyStyle.Number, "3", haptics) { onDigits("3") }
            PadKey(extraLabel, KeyStyle.Utility, extraDescription, haptics) { onNegate() }
        }
        Row(rowModifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
            PadKey("0", KeyStyle.Number, "0", haptics) { onDigits("0") }
            PadKey(decimalMark.toString(), KeyStyle.Number, "Decimal point", haptics) { onDecimal() }
            PadKey("00", KeyStyle.Number, "Double zero", haptics) { onDigits("00") }
            CalcKey(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                style = KeyStyle.Equals,
                description = "Copy result",
                onClick = onCopy,
            ) {
                Text(
                    text = "Copy",
                    color = c.onAccent,
                    fontFamily = ArnoldFonts.Display,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                )
            }
        }
    }
}
