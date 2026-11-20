@file:OptIn(ExperimentalLayoutApi::class)

package dev.arnoldsemase.calculator.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.arnoldsemase.calculator.engine.AgeResult
import dev.arnoldsemase.calculator.engine.DateDifference
import dev.arnoldsemase.calculator.ui.components.SegmentedChoice
import dev.arnoldsemase.calculator.ui.components.SubScreenBar
import dev.arnoldsemase.calculator.ui.components.rememberHaptics
import dev.arnoldsemase.calculator.ui.settings.SettingsViewModel
import dev.arnoldsemase.calculator.ui.theme.Arnold
import dev.arnoldsemase.calculator.ui.theme.ArnoldFonts
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.time.temporal.WeekFields
import java.util.Locale

private fun plural(n: Long, word: String) = "$n $word${if (n == 1L) "" else "s"}"

/** "1 month 12 days", skipping zero parts; "0 days" when there is nothing. */
private fun periodText(years: Int, months: Int, days: Int): String {
    val parts = buildList {
        if (years > 0) add(plural(years.toLong(), "year"))
        if (months > 0) add(plural(months.toLong(), "month"))
        if (days > 0) add(plural(days.toLong(), "day"))
    }
    return if (parts.isEmpty()) "0 days" else parts.joinToString(" ")
}

@Composable
fun DatesScreen(viewModel: DatesViewModel, settingsViewModel: SettingsViewModel, onBack: () -> Unit) {
    val c = Arnold.colors
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(settings.haptics)
    val locale = Locale.getDefault()
    val medium = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }

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
            SubScreenBar(title = "Dates", onBack = onBack)
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SegmentedChoice(
                    options = DateMode.entries.map { it to it.label },
                    selected = ui.mode,
                    onSelect = { haptics.tick(); viewModel.setMode(it) },
                    modifier = Modifier.fillMaxWidth(),
                )

                // ---- Date boxes
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (ui.mode) {
                        DateMode.DIFFERENCE -> {
                            DateBox("From", ui.from, medium, !ui.secondActive, Modifier.weight(1f)) { viewModel.selectField(false) }
                            DateBox("To", ui.to, medium, ui.secondActive, Modifier.weight(1f)) { viewModel.selectField(true) }
                        }
                        DateMode.ADD ->
                            DateBox("Start date", ui.start, medium, true, Modifier.weight(1f)) {}
                        DateMode.AGE -> {
                            DateBox("Date of birth", ui.birth, medium, !ui.secondActive, Modifier.weight(1f)) { viewModel.selectField(false) }
                            DateBox("As of", ui.asOf, medium, ui.secondActive, Modifier.weight(1f)) { viewModel.selectField(true) }
                        }
                    }
                }

                if (ui.mode == DateMode.ADD) {
                    SegmentedChoice(
                        options = listOf(false to "Add", true to "Subtract"),
                        selected = ui.subtract,
                        onSelect = { haptics.tick(); viewModel.setSubtract(it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AmountField("Years", ui.years, Modifier.weight(1f)) { viewModel.setAmount(AmountUnit.YEARS, it) }
                        AmountField("Months", ui.months, Modifier.weight(1f)) { viewModel.setAmount(AmountUnit.MONTHS, it) }
                        AmountField("Weeks", ui.weeks, Modifier.weight(1f)) { viewModel.setAmount(AmountUnit.WEEKS, it) }
                        AmountField("Days", ui.days, Modifier.weight(1f)) { viewModel.setAmount(AmountUnit.DAYS, it) }
                    }
                }

                // ---- Result
                when (ui.mode) {
                    DateMode.DIFFERENCE -> DifferenceResult(ui.difference, ui.includeEnd) {
                        haptics.tick()
                        viewModel.toggleIncludeEnd()
                    }
                    DateMode.ADD -> AddResult(ui.addedDate, ui.today, medium, locale)
                    DateMode.AGE -> AgeResultCard(ui.birth != null, ui.age, locale, medium)
                }

                CalendarCard(
                    month = ui.viewMonth,
                    from = if (ui.mode == DateMode.DIFFERENCE) ui.from else null,
                    to = if (ui.mode == DateMode.DIFFERENCE) ui.to else null,
                    selected = ui.activeDate,
                    today = ui.today,
                    locale = locale,
                    onPick = { haptics.tick(); viewModel.pick(it) },
                    onShift = { haptics.tick(); viewModel.shiftMonths(it) },
                    onToday = { haptics.confirm(); viewModel.pickToday() },
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ---- Date boxes and amounts ------------------------------------------------------------------------

@Composable
private fun DateBox(
    label: String,
    date: LocalDate?,
    format: DateTimeFormatter,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(18.dp)
    val text = date?.format(format) ?: "Pick a date"
    Column(
        modifier
            .clip(shape)
            .background(if (active) c.accentTint(0.08f) else c.surface)
            .border(1.dp, if (active) c.accent else c.outline.copy(alpha = 0.6f), shape)
            .clickable(role = Role.Button, onClickLabel = "Edit $label", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $text" },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, color = c.textSecondary, fontSize = 12.sp)
        Text(
            text,
            color = if (date == null) c.textSecondary else c.text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun AmountField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(14.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = c.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(c.accent),
            textStyle = TextStyle(
                color = c.text,
                fontFamily = ArnoldFonts.Display,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(shape)
                .background(c.surface)
                .border(1.dp, c.outline.copy(alpha = 0.7f), shape)
                .semantics { contentDescription = "$label amount" },
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) Text("0", color = c.textSecondary, fontSize = 20.sp, fontFamily = ArnoldFonts.Display)
                    inner()
                }
            },
        )
    }
}

// ---- Results ---------------------------------------------------------------------------------------

@Composable
private fun ResultShell(title: String, content: @Composable () -> Unit) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.accentTint(0.2f), c.accentTint(0.07f))))
            .border(1.dp, c.accent.copy(alpha = 0.35f), shape)
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = c.accentText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun BigText(text: String, size: Int = 44) {
    Text(
        text = text,
        color = Arnold.colors.text,
        fontFamily = ArnoldFonts.Display,
        fontWeight = FontWeight.Medium,
        fontSize = size.sp,
        letterSpacing = (-1).sp,
        lineHeight = (size * 1.1f).sp,
    )
}

@Composable
private fun Sub(text: String) = Text(text, color = Arnold.colors.textSecondary, fontSize = 13.sp)

@Composable
private fun Tag(text: String, onClick: (() -> Unit)? = null, selected: Boolean = false) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(50)
    var mod = Modifier
        .clip(shape)
        .background(if (selected) c.accentTint(0.25f) else c.surface)
        .border(1.dp, if (selected) c.accent.copy(alpha = 0.5f) else c.outline.copy(alpha = 0.7f), shape)
    if (onClick != null) mod = mod.clickable(role = Role.Switch, onClickLabel = text, onClick = onClick)
    Text(
        text = (if (onClick != null) (if (selected) "✓ " else "") else "") + text,
        color = if (selected) c.accentText else c.text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = mod.padding(horizontal = 12.dp, vertical = if (onClick != null) 10.dp else 6.dp),
    )
}

@Composable
private fun DifferenceResult(d: DateDifference, includeEnd: Boolean, onToggleEnd: () -> Unit) {
    ResultShell("Difference") {
        BigText(plural(d.days, "day"))
        Sub(periodText(d.years, d.months, d.dayPart) + if (d.backwards) " · the end date is before the start" else "")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (d.days >= 7) {
                Tag(plural(d.weeks, "week") + if (d.extraDays > 0) " ${plural(d.extraDays, "day")}" else "")
            }
            Tag("${NumberFormat.getIntegerInstance().format(d.workingDays)} working days")
            Tag("Include end date", onClick = onToggleEnd, selected = includeEnd)
        }
    }
}

@Composable
private fun AddResult(date: LocalDate?, today: LocalDate, format: DateTimeFormatter, locale: Locale) {
    ResultShell("Result") {
        if (date == null) {
            BigText("—")
            Sub("That date is out of range")
            return@ResultShell
        }
        BigText(date.format(format), size = 34)
        val away = ChronoUnit.DAYS.between(today, date)
        val relative = when {
            away == 0L -> "today"
            away == 1L -> "tomorrow"
            away == -1L -> "yesterday"
            away > 0 -> "in ${plural(away, "day")}"
            else -> "${plural(-away, "day")} ago"
        }
        Sub("${date.dayOfWeek.getDisplayName(DateTextStyle.FULL, locale)} · $relative")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag("Day ${date.dayOfYear} of the year")
            Tag("Week ${date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}")
        }
    }
}

@Composable
private fun AgeResultCard(hasBirth: Boolean, age: AgeResult?, locale: Locale, format: DateTimeFormatter) {
    ResultShell("Age") {
        when {
            !hasBirth -> {
                BigText("—")
                Sub("Pick a date of birth below")
            }
            age == null -> {
                BigText("—")
                Sub("The date of birth is after the as-of date")
            }
            else -> {
                BigText(plural(age.years.toLong(), "year"))
                Sub(periodText(0, age.months, age.days))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tag("${NumberFormat.getIntegerInstance().format(age.totalDays)} days old")
                    val day = age.nextBirthday.dayOfWeek.getDisplayName(DateTextStyle.SHORT, locale)
                    Tag(
                        if (age.daysToNextBirthday == 0L) "Birthday today"
                        else "Next birthday in ${plural(age.daysToNextBirthday, "day")} · $day",
                    )
                }
            }
        }
    }
}

// ---- Calendar --------------------------------------------------------------------------------------

@Composable
private fun CalendarCard(
    month: YearMonth,
    from: LocalDate?,
    to: LocalDate?,
    selected: LocalDate?,
    today: LocalDate,
    locale: Locale,
    onPick: (LocalDate) -> Unit,
    onShift: (Long) -> Unit,
    onToday: () -> Unit,
) {
    val c = Arnold.colors
    val shape = RoundedCornerShape(22.dp)
    val firstDay = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val title = remember(month, locale) { month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)) }
    val lead = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
    val gridStart = month.atDay(1).minusDays(lead.toLong())
    val rangeLow: LocalDate? = if (from != null && to != null) (if (from.isBefore(to)) from else to) else null
    val rangeHigh: LocalDate? = if (from != null && to != null) (if (from.isBefore(to)) to else from) else null

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outline.copy(alpha = 0.6f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NavButton("«", "Previous year") { onShift(-12) }
            NavButton("‹", "Previous month") { onShift(-1) }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = c.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
            NavButton("›", "Next month") { onShift(1) }
            NavButton("»", "Next year") { onShift(12) }
        }
        Row(Modifier.fillMaxWidth()) {
            for (i in 0 until 7) {
                val dow = DayOfWeek.of((firstDay.value - 1 + i) % 7 + 1)
                Box(Modifier.weight(1f).height(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        dow.getDisplayName(DateTextStyle.NARROW, locale),
                        color = c.textSecondary,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        for (week in 0 until 6) {
            Row(Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val date = gridStart.plusDays((week * 7 + i).toLong())
                    DayCell(
                        date = date,
                        inMonth = YearMonth.from(date) == month,
                        isEnd = date == from || date == to || (from == null && date == selected),
                        inRange = rangeLow != null && rangeHigh != null && date.isAfter(rangeLow) && date.isBefore(rangeHigh),
                        isToday = date == today,
                        modifier = Modifier.weight(1f),
                        onClick = { onPick(date) },
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Tag("Today", onClick = onToday)
        }
    }
}

@Composable
private fun NavButton(glyph: String, description: String, onClick: () -> Unit) {
    val c = Arnold.colors
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isEnd: Boolean,
    inRange: Boolean,
    isToday: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = Arnold.colors
    val dayShape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(42.dp)
            .background(if (inRange) c.accentTint(0.2f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(role = Role.Button, onClickLabel = "Pick ${date}", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(dayShape)
                .then(if (isEnd) Modifier.background(Brush.linearGradient(listOf(c.accentSecondary, c.accent))) else Modifier)
                .then(if (isToday && !isEnd) Modifier.border(1.dp, c.accent, dayShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = when {
                    isEnd -> c.onAccent
                    !inMonth -> c.textSecondary.copy(alpha = 0.45f)
                    inRange -> c.accentText
                    else -> c.text
                },
                fontSize = 14.sp,
                fontWeight = if (isEnd || isToday) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
