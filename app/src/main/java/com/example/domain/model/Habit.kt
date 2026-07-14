package com.example.domain.model

import androidx.compose.runtime.Immutable
import java.time.DayOfWeek

@Immutable
data class Habit(
    val id: Int = 0,
    val name: String,
    val description: String,
    val durationDays: Int,
    val colorHex: String,
    val isActive: Boolean,
    val reminderTimes: List<String>, // "HH:mm" e.g., ["08:00", "14:00"]
    val createdAt: Long,
    val startedAt: Long?,
    val status: HabitStatus = HabitStatus.ACTIVE,
    val cycleStartDate: Long = startedAt ?: createdAt,
    val cycleEndDate: Long = cycleStartDate + durationDays * 24L * 60L * 60L * 1000L,
    val inactiveDaysCount: Int = 0,
    val activeDays: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val inactiveSinceTimestamp: Long? = null
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
        val end = java.time.Instant.ofEpochMilli(cycleEndDate)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

        return !date.isBefore(start) && !date.isAfter(end)
    }

    /**
     * Returns the count of days within the habit's duration that are actually
     * scheduled based on [activeDays]. For an unrestricted habit, this equals [durationDays].
     */
    fun getScheduledDaysCount(): Int {
        val start = java.time.Instant.ofEpochMilli(cycleStartDate)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val end = java.time.Instant.ofEpochMilli(cycleEndDate)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

        var scheduledCount = 0
        var curr = start
        while (!curr.isAfter(end)) {
            if (isActiveOnDate(curr)) {
                scheduledCount++
            }
            curr = curr.plusDays(1)
        }
        return scheduledCount
    }
}

