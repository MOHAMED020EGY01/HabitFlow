package com.example.feature.habit.domain

import androidx.compose.runtime.Immutable
import com.example.core.model.domain.Habit
import com.example.core.model.domain.HabitLog
import com.example.core.repository.HabitRepository
import com.example.core.util.StreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

@Immutable
data class HabitDetails(
    val habit: Habit,
    val logs: List<HabitLog>,
    val completedDays: Int,
    val missedDays: Int,
    val streakDays: Int
)

class GetHabitDetailsUseCase(private val repository: HabitRepository) {
    suspend operator fun invoke(habitId: Int): Flow<HabitDetails?> {
        val habitFlow = repository.getHabitByIdFlow(habitId)
        val logsFlow = repository.getLogsForHabit(habitId)

        return combine(habitFlow, logsFlow) { habit, logs ->
            if (habit == null) return@combine null
            val completedDays = logs.count { it.completed }
            
            // missedDays: count of actual MISS logs (populated by daily rollover,
            // which now only creates MISS entries for days in activeDays).
            // This is more accurate than computing from start→today diff because
            // it correctly handles activeDays, cycle boundaries, and INACTIVE pauses.
            val missedDays = logs.count { it.state == "MISS" }

            // streakDays calculation: consecutive completed days ending today (check backwards from today)
            // activeDays-aware: non-scheduled days do not count or break the streak
            val completedDatesSet = logs.filter { it.completed }.map { it.logDate }.toSet()
            val streakDays = StreakCalculator.calculateStreakFromDates(
                completedDates = completedDatesSet,
                activeDays = habit.activeDays,
                habitId = habit.id
            )

            HabitDetails(habit, logs, completedDays, missedDays, streakDays)
        }
    }
}
