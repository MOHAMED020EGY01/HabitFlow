package com.example.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.util.Locale

import com.example.core.util.DayFormatter

// ─────────────────────────────────────────────────────────────────────────────
// Day letter maps
// ─────────────────────────────────────────────────────────────────────────────

/** Display order: Sunday → Saturday (Unified) */
private val DAY_ORDER = listOf(
    DayOfWeek.SUNDAY,
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY
)

/**
 * Returns the full day name based on the current
 * display language.
 */
private fun dayLabel(day: DayOfWeek, locale: Locale): String {
    return com.example.core.util.AppFormatters.getFullDayName(day, locale)
}

// ─────────────────────────────────────────────────────────────────────────────
// DaysOfWeekSelector
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A reusable days-of-week selector matching the reference screenshots.
 *
 * ## Layout
 * - Header row: [Checkbox] + "Daily" / "كل يوم" label.
 * - Below: a row of 7 rounded chips, one per day.
 *
 * ## Behaviour
 * - **Daily checked:** all chips shown in ACTIVE (filled) state; tapping a
 *   chip un-checks Daily, switches to custom mode, and selects only that day.
 * - **Daily unchecked:** chips toggle independently; >= 1 day must stay selected.
 * - **Re-checking Daily:** selects all 7 days and shows all chips as filled.
 *
 * ## RTL handling
 * The chip row uses a standard [Row] — when [LocalLayoutDirection] is RTL,
 * Compose mirrors the layout automatically so the row reads right-to-left.
 * Each chip's [DayOfWeek] identity is bound by [DAY_ORDER] index, *not* by
 * visual position, so the mapping is always correct regardless of direction.
 *
 * @param selectedDays  The currently selected set of days.
 * @param onDaysChanged Called when the selection changes.
 * @param modifier      Outer modifier.
 */
@Composable
fun DaysOfWeekSelector(
    selectedDays: Set<DayOfWeek>,
    onDaysChanged: (Set<DayOfWeek>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    
    val app = context.applicationContext as com.example.app.HabitApplication
    val langCode = app.currentLanguageCode
    val locale = remember(langCode) { com.example.core.util.LocaleDirectionHelper.getLocale(langCode) }

    // Determine if "Daily" mode is active (all 7 days selected).
    val allDays = DayOfWeek.values().toSet()
    val isDaily = selectedDays.size == allDays.size

    // ── Chip colours ─────────────────────────────────────────────────
    val activeChipBg = MaterialTheme.colorScheme.secondary   // cyan/teal fill
    val activeChipText = if (isDark) Color(0xFF0F0F1A) else Color.White
    val inactiveChipBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val inactiveChipBorder = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
    val inactiveChipText = MaterialTheme.colorScheme.secondary

    val chipShape = RoundedCornerShape(8.dp)

    // Tracks whether the user tried to deselect the last remaining day
    // (blocked by the size <= 1 guard). Only show the hint when this is true.
    var showDeselectBlockedHint by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // ── Header: Checkbox + "Daily" ───────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showDeselectBlockedHint = false
                    onDaysChanged(if (isDaily) setOf(DAY_ORDER[0]) else allDays)
                }
                .padding(vertical = 4.dp)
        ) {
            Checkbox(
                checked = isDaily,
                onCheckedChange = { checked ->
                    showDeselectBlockedHint = false
                    onDaysChanged(if (checked) allDays else setOf(DAY_ORDER[0]))
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.secondary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(com.example.R.string.daily),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Chip row ─────────────────────────────────────────────────
        // Row respects LocalLayoutDirection — in RTL the chips render
        // right-to-left automatically, which matches Arabic calendar
        // reading order.  The DayOfWeek identity at each slot is
        // determined by DAY_ORDER[index], so it never changes.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DAY_ORDER.forEachIndexed { index, day ->
                val isSelected = day in selectedDays
                val label = dayLabel(day, locale)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(chipShape)
                        .then(
                            if (isSelected) {
                                Modifier.background(activeChipBg)
                            } else {
                                Modifier
                                    .background(inactiveChipBg)
                                    .border(1.5.dp, inactiveChipBorder, chipShape)
                            }
                        )
                        .clickable {
                            if (isDaily) {
                                // Daily ON → tap switches to custom mode with only this day
                                showDeselectBlockedHint = false
                                onDaysChanged(setOf(day))
                            } else {
                                // Daily OFF → toggle this day
                                val newSet = if (isSelected) {
                                    // Block deselection of the last remaining day
                                    if (selectedDays.size <= 1) {
                                        showDeselectBlockedHint = true
                                        return@clickable  // no-op
                                    }
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                                showDeselectBlockedHint = false
                                onDaysChanged(newSet)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 8.sp, // significantly smaller to fit full names
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) activeChipText else inactiveChipText,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        // ── Subtle hint when the user tried to deselect the last day but was blocked ──
        if (showDeselectBlockedHint) {
            InlineErrorBanner(
                message = stringResource(com.example.R.string.days_selector_at_least_one),
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
