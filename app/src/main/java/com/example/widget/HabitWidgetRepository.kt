package com.example.widget

import com.example.domain.repository.HabitRepository
import java.time.DayOfWeek

/**
 * Fetches widget display data for the first 6 active habits.
 * Reuses the existing HabitRepository.
 */
class HabitWidgetRepository(
    private val habitRepository: HabitRepository
) {

    data class WidgetHabitData(
        val habitId: Int,
        val name: String,
        val colorHex: String,
        val daysCompleted: Int,
        val totalDays: Int,
        val daysRemaining: Int,
        val progressPercent: Float,
        val isCompletedToday: Boolean,
        val reminderTimes: List<String>,
        val activeDays: Set<DayOfWeek>,
        val inactiveSinceTimestamp: Long? = null
    )

    /**
     * Returns up to 6 active habits in the same order shown on the Home screen.
     */
    suspend fun getTopActiveHabitsForWidgets(): List<WidgetHabitData> {
        val today = java.time.LocalDate.now().toString()
        return habitRepository.getTopActiveHabitsForWidgets(today).map { info ->
            WidgetHabitData(
                habitId = info.habitId,
                name = info.name,
                colorHex = info.colorHex,
                daysCompleted = info.daysCompleted,
                totalDays = info.totalDays,
                daysRemaining = info.daysRemaining,
                progressPercent = info.progressPercent,
                isCompletedToday = info.isCompletedToday,
                reminderTimes = info.reminderTimes,
                activeDays = info.activeDays
            )
        }
    }

    suspend fun markHabitDoneToday(habitId: Int) {
        val today = java.time.LocalDate.now().toString()
        habitRepository.logHabitCompletion(habitId, today, true)
    }

    suspend fun getRecentInactiveHabitsForWidgets(limit: Int): List<WidgetHabitData> {
        return habitRepository.getRecentInactiveHabits(limit).map { habit ->
            // For inactive habits, we might not need all fields, but we'll reuse WidgetHabitData
            WidgetHabitData(
                habitId = habit.id,
                name = habit.name,
                colorHex = habit.colorHex,
                daysCompleted = 0, // Not strictly needed for this widget
                totalDays = habit.durationDays,
                daysRemaining = 0,
                progressPercent = 0f,
                isCompletedToday = false,
                reminderTimes = habit.reminderTimes,
                activeDays = habit.activeDays,
                inactiveSinceTimestamp = habit.inactiveSinceTimestamp
            )
        }
    }
}
