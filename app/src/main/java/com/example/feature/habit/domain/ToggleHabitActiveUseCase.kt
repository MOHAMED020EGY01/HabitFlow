package com.example.feature.habit.domain

import com.example.core.model.domain.ActivationResult
import com.example.core.model.domain.MAX_ACTIVE_HABITS
import com.example.core.repository.HabitRepository

class ToggleHabitActiveUseCase(
    private val repository: HabitRepository,
    private val context: android.content.Context
) {
    suspend operator fun invoke(habitId: Int, makeActive: Boolean): ActivationResult {
        val habit = repository.getHabitById(habitId) ?: return ActivationResult.NotApplicable

        if (!makeActive) {
            repository.setHabitActive(
                habitId, 
                isActive = false, 
                startedAt = null, 
                status = com.example.core.model.domain.HabitStatus.INACTIVE,
                inactiveSince = System.currentTimeMillis()
            )
            com.example.core.infrastructure.widget.HabitWidgetSyncUpdater.updateNow(context)
            return ActivationResult.NotApplicable
        }

        val activeCount = repository.getActiveHabitsCount()

        return if (activeCount >= MAX_ACTIVE_HABITS) {
            ActivationResult.SavedAsInactive(activeCount)
        } else {
            val isRestart = habit.status == com.example.core.model.domain.HabitStatus.COMPLETE || 
                            habit.status == com.example.core.model.domain.HabitStatus.FAILURE

            if (isRestart) {
                // Perform Restart logic: Reset dates and clear old logs
                val todayMillis = System.currentTimeMillis()
                val newEndDate = todayMillis + (habit.durationDays * 24L * 60L * 60L * 1000L)
                val restartedHabit = habit.copy(
                    status = com.example.core.model.domain.HabitStatus.ACTIVE,
                    isActive = true,
                    cycleStartDate = todayMillis,
                    cycleEndDate = newEndDate,
                    inactiveDaysCount = 0,
                    inactiveSinceTimestamp = null,
                    startedAt = todayMillis
                )
                repository.deleteLogsForHabit(habitId)
                repository.updateHabit(restartedHabit)
            } else {
                repository.setHabitActive(
                    habitId, 
                    isActive = true, 
                    startedAt = System.currentTimeMillis(),
                    status = com.example.core.model.domain.HabitStatus.ACTIVE,
                    inactiveSince = null
                )
            }

            com.example.core.infrastructure.widget.HabitWidgetSyncUpdater.updateNow(context)
            ActivationResult.Activated
        }
    }
}
