package com.example.domain.usecase

import com.example.domain.repository.HabitRepository
import com.example.domain.repository.ReminderTimeEntry
import java.time.DayOfWeek
import kotlin.math.abs
import kotlin.math.min

class ValidateReminderTimeUseCase(
    private val habitRepository: HabitRepository
) {
    companion object {
        const val MIN_GAP_MINUTES = 10
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Conflict(
            val conflictingHabitName: String,
            val conflictingTime: String
        ) : ValidationResult()
    }

    /**
     * @param proposedTime       the time being validated, format "HH:mm"
     * @param proposedActiveDays the set of days this reminder will fire on
     * @param editingHabitId     ID of the habit being edited (null if new)
     * @param otherTimesOfHabit  other reminder times already assigned to this habit
     */
    suspend fun validate(
        proposedTime: String,
        proposedActiveDays: Set<DayOfWeek>,
        editingHabitId: Int? = null,
        otherTimesOfHabit: List<String> = emptyList()
    ): ValidationResult {
        val proposedMinutes = toMinutesSinceMidnight(proposedTime)

        // 1. Check against other times of the SAME habit
        for (otherTime in otherTimesOfHabit) {
            if (otherTime == proposedTime) continue // should not happen if called correctly
            val otherMinutes = toMinutesSinceMidnight(otherTime)
            if (circularMinuteGap(proposedMinutes, otherMinutes) < MIN_GAP_MINUTES) {
                return ValidationResult.Conflict(
                    conflictingHabitName = "this habit",
                    conflictingTime = otherTime
                )
            }
        }

        // 2. Check against every OTHER active habit in the database
        val allHabits = habitRepository.getAllHabitsSync()
        for (habit in allHabits) {
            // Only check other active habits
            if (!habit.isActive || habit.id == editingHabitId) continue

            // Only check if they share at least one active day
            val sharedDays = habit.activeDays.intersect(proposedActiveDays)
            if (sharedDays.isEmpty()) continue

            for (existingTime in habit.reminderTimes) {
                val existingMinutes = toMinutesSinceMidnight(existingTime)
                if (circularMinuteGap(proposedMinutes, existingMinutes) < MIN_GAP_MINUTES) {
                    return ValidationResult.Conflict(
                        conflictingHabitName = habit.name,
                        conflictingTime = existingTime
                    )
                }
            }
        }

        return ValidationResult.Valid
    }

    private fun toMinutesSinceMidnight(time: String): Int {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hour * 60 + minute
    }

    private fun circularMinuteGap(a: Int, b: Int): Int {
        val diff = abs(a - b)
        return min(diff, 1440 - diff)
    }
}
