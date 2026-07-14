package com.example.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Habit
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import android.widget.Toast
import com.example.HabitApplication
import com.example.domain.util.NextReminderCalculator
import androidx.compose.ui.platform.LocalContext
import java.time.DayOfWeek
import java.util.Locale

@Composable
fun HabitCard(
    habit: Habit,
    completedDays: Int,
    isCheckedToday: Boolean,
    onCheckedToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onActivateClick: (() -> Unit)? = null,
    streakDays: Int = 0,
    animationDelayMs: Int = 0
) {
    val habitColor = try {
        Color(android.graphics.Color.parseColor(habit.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    val context = LocalContext.current
    val isArabic = remember {
        val app = context.applicationContext as? com.example.HabitApplication
        app?.currentLanguageCode == "ar" || 
        (app?.currentLanguageCode == "system" && androidx.core.os.ConfigurationCompat.getLocales(context.resources.configuration).get(0)?.language == "ar")
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            currentTime = LocalDateTime.now()
        }
    }

    val nextReminder = remember(habit.reminderTimes, currentTime, habit.activeDays) {
        val parsedTimes = habit.reminderTimes.mapNotNull {
            try {
                val parts = it.split(":")
                LocalTime.of(parts[0].toInt(), parts[1].toInt())
            } catch (e: Exception) {
                null
            }
        }
        NextReminderCalculator.getNextReminderDateTime(parsedTimes, currentTime, habit.activeDays)
    }

    val scheduleText = remember(habit.activeDays, isArabic) {
        if (habit.activeDays.size == 7) {
            context.getString(com.example.R.string.daily)
        } else {
            val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
            habit.activeDays.sortedBy { if (it == DayOfWeek.SUNDAY) 0 else it.value } // Sunday first
                .joinToString(", ") { com.example.util.AppFormatters.getFullDayName(it, locale) }
        }
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = habitColor.copy(alpha = 0.25f),
                spotColor = habitColor.copy(alpha = 0.35f)
            ),
        habitColor = habitColor,
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        animationDelayMs = animationDelayMs
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Color Left Accent Bar (Clipped perfectly with the Card)
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(habitColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = habit.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (habit.description.isNotEmpty()) {
                        Text(
                            text = habit.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (nextReminder != null) {
                        val timeStr = NextReminderCalculator.formatTime(nextReminder, isArabic)
                        Text(
                            text = "$scheduleText • $timeStr",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val scheduledTotalDays = habit.getScheduledDaysCount()
                    val progress = if (scheduledTotalDays > 0) completedDays.toFloat() / scheduledTotalDays else 0f
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            color = habitColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = com.example.util.AppFormatters.forceWesternDigits(
                                context.getString(com.example.R.string.habit_card_days_progress, completedDays, scheduledTotalDays)
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Streak Line Row (Uses habit's own accent color)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = com.example.util.AppFormatters.forceWesternDigits(
                                context.getString(com.example.R.string.habit_streak_days, streakDays)
                            ),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = habitColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Check or Activate button
                if (!habit.isActive && onActivateClick != null) {
                    IconButton(
                        onClick = onActivateClick,
                        modifier = Modifier.size(48.dp).testTag("quick_activate_${habit.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Activate",
                            tint = habitColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCheckedToday) habitColor
                                else Color.Transparent
                            )
                            .border(
                                width = 2.dp,
                                color = if (isCheckedToday) habitColor else habitColor.copy(alpha = 0.6f),
                                shape = CircleShape
                            )
                            .clickable {
                                if (habit.isActiveToday()) {
                                    onCheckedToggle(!isCheckedToday)
                                } else if (!isCheckedToday) {
                                    // Block marking as Done on non-active days
                                    val message = if (isArabic) 
                                        "هذه العادة غير مجدولة لليوم" 
                                    else 
                                        "This habit isn't scheduled for today"
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                } else {
                                    // Allow un-checking even if it was a non-active day (unlikely but safe)
                                    onCheckedToggle(false)
                                }
                            }
                            .testTag("toggle_check_${habit.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Toggle Complete",
                            tint = if (isCheckedToday) MaterialTheme.colorScheme.surface else habitColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

