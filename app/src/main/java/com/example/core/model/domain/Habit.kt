package com.example.core.model.domain

import androidx.compose.runtime.Immutable
import java.time.DayOfWeek

@Immutable
data class Habit(
    val id: Int = 0,
    val name: String,
    val description: String,
    val durationDays: Int, // Number of calendar days if type is CALENDAR
    val durationType: HabitDurationType = HabitDurationType.CALENDAR,
    val targetOccurrenceCount: Int? = null, // Number of target completions if type is OCCURRENCE
    val colorHex: String,
    val isActive: Boolean,
    val reminderTimes: List<String>, // "HH:mm" e.g., ["08:00", "14:00"]
    val createdAt: Long,
    val startedAt: Long?,
    val status: HabitStatus = HabitStatus.ACTIVE,
    val cycleStartDate: Long = startedAt ?: createdAt,
    val cycleEndDate: Long? = if (durationType == HabitDurationType.CALENDAR) {
        cycleStartDate + durationDays * 24L * 60L * 60L * 1000L
    } else null,
    val inactiveDaysCount: Int = 0,
    val activeDays: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val inactiveSinceTimestamp: Long? = null,
    val reminderVoice: String = "DEFAULT"
) {
    /** Convenience: returns true if the habit runs on [day]. */
    fun isActiveOn(day: DayOfWeek): Boolean = day in activeDays

    /** Convenience: returns true if the habit runs on today (device local date). */
    fun isActiveToday(): Boolean =
        isScheduledOn(java.time.LocalDate.now())

    /** Convenience: returns true if the habit runs on the given [date]. */
    fun isActiveOnDate(date: java.time.LocalDate): Boolean =
        isActiveOn(date.dayOfWeek)

    /**
     * Checks if the habit is scheduled for a specific date.
     * Reuses existing domain logic (activeDays) and applies date-range cutoff.
     */
    fun isScheduledOn(date: java.time.LocalDate): Boolean {
        // 1. Day-of-week check
        if (date.dayOfWeek !in activeDays) return false

        // 2. Date-range validity check
        val start = java.time.Instant.ofEpochMilli(cycleStartDate)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        
        if (date.isBefore(start)) return false

        // For OCCURRENCE type, there is no end date cutoff based on calendar
        if (durationType == HabitDurationType.OCCURRENCE) return true

        val end = cycleEndDate?.let {
            java.time.Instant.ofEpochMilli(it)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        }

        return end == null || date.isBefore(end)
    }

    /**
     * Returns the count of days within the habit's duration that are actually
     * scheduled based on [activeDays]. 
     * For CALENDAR: days within the range.
     * For OCCURRENCE: returns [targetOccurrenceCount].
     */
    fun getScheduledDaysCount(): Int {
        if (durationType == HabitDurationType.OCCURRENCE) return targetOccurrenceCount ?: 0

        val start = java.time.Instant.ofEpochMilli(cycleStartDate)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val end = cycleEndDate?.let {
            java.time.Instant.ofEpochMilli(it)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        } ?: return 0

        var scheduledCount = 0
        var curr = start
        while (curr.isBefore(end)) {
            if (isActiveOnDate(curr)) {
                scheduledCount++
            }
            curr = curr.plusDays(1)
        }
        return scheduledCount
    }

    /**
     * Returns the projected end date for the habit.
     * For CALENDAR: returns the last scheduled day BEFORE or ON the cycleEndDate.
     * For OCCURRENCE: returns the date of the N-th occurrence (where N = target count).
     */
    fun getProjectedEndDate(): java.time.LocalDate? {
        val start = java.time.Instant.ofEpochMilli(cycleStartDate)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

        if (durationType == HabitDurationType.CALENDAR) {
            val end = cycleEndDate?.let {
                java.time.Instant.ofEpochMilli(it)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            } ?: return null
            
            // Find the last day that matches activeDays within the range [start, end)
            // Note: isScheduledOn uses date.isBefore(end), so the last possible day is end.minusDays(1)
            var curr = end.minusDays(1)
            while (curr.isAfter(start) || curr.isEqual(start)) {
                if (isActiveOnDate(curr)) return curr
                curr = curr.minusDays(1)
            }
            return start // Fallback
        }

        val target = targetOccurrenceCount ?: return null
        if (activeDays.isEmpty()) return null

        var found = 0
        var curr = start
        // Limit lookahead to 5 years to prevent infinite loops if something goes wrong
        val maxDate = start.plusYears(5)
        
        while (found < target && curr.isBefore(maxDate)) {
            if (isActiveOnDate(curr)) {
                found++
                if (found == target) return curr
            }
            curr = curr.plusDays(1)
        }
        return curr
    }
}

