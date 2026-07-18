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

    data class HabitStatsSummary(
        val totalCount: Int,
        val activeCount: Int,
        val inactiveCount: Int,
        val completedCount: Int,
        val failedCount: Int,
        val bestStreakName: String,
        val bestStreakCount: Int,
        val bestStreakColor: String
    )

    suspend fun getHabitStatsSummary(): HabitStatsSummary {
        val habits = habitRepository.getAllHabitsSync()
        val allLogs = habitRepository.getAllLogsSync().groupBy { it.habitId }

        var bestStreakName = "None"
        var bestStreakCount = 0
        var bestStreakColor = "#7C4DFF"

        habits.forEach { habit ->
            val logs = allLogs[habit.id] ?: emptyList()
            val completedDates = logs.filter { it.completed }.map { it.logDate }.toSet()
            val streak = com.example.domain.util.StreakCalculator.calculateStreakFromDates(
                completedDates = completedDates,
                activeDays = habit.activeDays,
                habitId = habit.id
            )
            if (streak > bestStreakCount) {
                bestStreakCount = streak
                bestStreakName = habit.name
                bestStreakColor = habit.colorHex
            }
        }

        // Fallback for best streak
        if (bestStreakCount == 0 && habits.isNotEmpty()) {
            val first = habits.first()
            bestStreakName = first.name
            bestStreakColor = first.colorHex
        }

        return HabitStatsSummary(
            totalCount = habits.size,
            activeCount = habits.count { it.status == com.example.domain.model.HabitStatus.ACTIVE },
            inactiveCount = habits.count { it.status == com.example.domain.model.HabitStatus.INACTIVE },
            completedCount = habits.count { it.status == com.example.domain.model.HabitStatus.COMPLETE },
            failedCount = habits.count { it.status == com.example.domain.model.HabitStatus.FAILURE },
            bestStreakName = bestStreakName,
            bestStreakCount = bestStreakCount,
            bestStreakColor = bestStreakColor
        )
    }
}
