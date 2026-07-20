package com.example.core.util

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

import android.content.Context
import com.example.R

object NextReminderCalculator {

    /**
     * Finds the nearest upcoming reminder occurrence.
     * Respects [activeDays] by searching up to 7 days ahead.
     */
    fun getNextReminderDateTime(
        reminderTimes: List<LocalTime>,
        now: LocalDateTime = LocalDateTime.now(),
        activeDays: Set<DayOfWeek> = DayOfWeek.values().toSet()
    ): LocalDateTime? {
        if (reminderTimes.isEmpty() || activeDays.isEmpty()) return null
        
        val sortedTimes = reminderTimes.sorted()
        
        // Search through the next 7 days for an active day with an upcoming reminder
        for (dayOffset in 0..7) {
            val targetDate = now.toLocalDate().plusDays(dayOffset.toLong())
            if (targetDate.dayOfWeek !in activeDays) continue
            
            // On the same day, only consider times that haven't passed yet
            // Added .withSecond(0).withNano(0) to now.toLocalTime() to prevent 
            // skipping reminders that are in the CURRENT minute.
            val nowTime = now.toLocalTime().withSecond(0).withNano(0)
            val potentialTimes = if (dayOffset == 0) {
                sortedTimes.filter { !it.isBefore(nowTime) }
            } else {
                sortedTimes
            }
            
            if (potentialTimes.isNotEmpty()) {
                return targetDate.atTime(potentialTimes.first()).withSecond(0).withNano(0)
            }
        }
        
        return null
    }

    /**
     * Formats the duration until [target] in a localized way.
     */
    fun formatDuration(context: Context, target: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String {
        val duration = Duration.between(now, target)
        val hours = duration.toHours().toInt()
        val minutes = (duration.toMinutes() % 60).toInt().coerceAtLeast(0)

        return if (hours > 0) {
            context.getString(R.string.next_reminder_duration_hm, hours, minutes)
        } else {
            context.getString(R.string.next_reminder_duration_m, minutes)
        }
    }

    /**
     * Formats the time in a localized way.
     */
    fun formatTime(time: LocalDateTime, isArabic: Boolean): String {
        return com.example.core.util.AppFormatters.formatTime(time.toLocalTime(), isArabic)
    }
}
