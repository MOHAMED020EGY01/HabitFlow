package com.example.domain.usecase

import androidx.compose.runtime.Immutable
import com.example.domain.model.Habit
import com.example.domain.model.HabitWithCompletion
import com.example.domain.model.HabitLogDate
import com.example.domain.repository.HabitRepository
import com.example.domain.util.StreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate

@Immutable
data class HabitsSummary(
    val totalHabits: Int,
    val activeHabits: Int,
    val completedHabits: Int, // finished all days
    val bestStreakHabitName: String,
    val bestStreakCount: Int,
    val bestStreakColorHex: String,
    val leaderboard: List<LeaderboardItem>
)

@Immutable
data class LeaderboardItem(
    val habit: Habit,
    val completedDaysCount: Int,
    val rank: Int
)

class GetHabitsSummaryUseCase(private val repository: HabitRepository) {
    operator fun invoke(): Flow<HabitsSummary> {
        val todayStr = LocalDate.now().toString()
        val habitsFlow = repository.getAllHabitsWithCompletion(todayStr)
        val logsFlow = repository.getCompletedLogDates()

        return combine(habitsFlow, logsFlow) { habitsWithCompletion, completedLogs ->
            val totalHabits = habitsWithCompletion.size
            val activeHabits = habitsWithCompletion.count { it.habit.isActive }

            val completedLogsGrouped = completedLogs.groupBy { it.habitId }

            // Leaderboard item calculation
            val leaderboardItems = habitsWithCompletion.map { item ->
                LeaderboardItem(item.habit, item.completedCount, 0)
            }.sortedByDescending { it.completedDaysCount }

            // Apply rank
            val rankedLeaderboard = leaderboardItems.mapIndexed { index, item ->
                item.copy(rank = index + 1)
            }

            // completedHabits: habits that have officially transitioned to COMPLETE status.
            val completedHabits = habitsWithCompletion.count { item ->
                item.habit.status == com.example.domain.model.HabitStatus.COMPLETE
            }

            // Best streak calculation
            var bestStreakName = "No Habit"
            var bestStreakCount = 0
            var bestStreakColor = "#7C4DFF"

            habitsWithCompletion.forEach { item ->
                val logsForHabit = completedLogsGrouped[item.habit.id] ?: emptyList()
                val completedDatesSet = logsForHabit.map { it.logDate }.toSet()
                val streak = StreakCalculator.calculateStreakFromDates(
                    completedDates = completedDatesSet,
                    activeDays = item.habit.activeDays,
                    habitId = item.habit.id
                )
                if (streak > bestStreakCount) {
                    bestStreakCount = streak
                    bestStreakName = item.habit.name
                    bestStreakColor = item.habit.colorHex
                }
            }

            // Fallback for best streak if no streaks yet
            if (bestStreakCount == 0 && habitsWithCompletion.isNotEmpty()) {
                val firstHabit = habitsWithCompletion.first().habit
                bestStreakName = firstHabit.name
                bestStreakColor = firstHabit.colorHex
            }

            HabitsSummary(
                totalHabits = totalHabits,
                activeHabits = activeHabits,
                completedHabits = completedHabits,
                bestStreakHabitName = bestStreakName,
                bestStreakCount = bestStreakCount,
                bestStreakColorHex = bestStreakColor,
                leaderboard = rankedLeaderboard
            )
        }.flowOn(Dispatchers.Default)
    }
}
