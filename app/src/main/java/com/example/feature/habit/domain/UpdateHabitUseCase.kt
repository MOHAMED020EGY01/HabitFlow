package com.example.feature.habit.domain

import android.content.Context
import com.example.core.model.domain.ActivationResult
import com.example.core.model.domain.MAX_ACTIVE_HABITS
import com.example.core.model.domain.Habit
import com.example.core.repository.HabitRepository

class UpdateHabitUseCase(
    private val repository: HabitRepository,
    private val context: Context
) {
    suspend operator fun invoke(
        updatedHabit: Habit,
        wasActiveBefore: Boolean
    ): Pair<ActivationResult, Habit> {
        android.util.Log.d("HabitWidgetSync", "[LOG] UpdateHabitUseCase: Preparing to update habit. Timestamp: ${System.currentTimeMillis()}")
        val isNewlyActivated = updatedHabit.isActive && !wasActiveBefore

        if (!isNewlyActivated) {
            repository.updateHabit(updatedHabit)
            android.util.Log.d("HabitWidgetSync", "[LOG] UpdateHabitUseCase: DB write completed (not newly activated). Timestamp: ${System.currentTimeMillis()}")
            com.example.core.infrastructure.widget.HabitWidgetSyncUpdater.updateNow(context)
            return Pair(ActivationResult.NotApplicable, updatedHabit)
        }

        val activeCount = repository.getActiveHabitsCount()

        return if (activeCount >= MAX_ACTIVE_HABITS) {
            val forcedInactive = updatedHabit.copy(isActive = false, startedAt = null)
            repository.updateHabit(forcedInactive)
            android.util.Log.d("HabitWidgetSync", "[LOG] UpdateHabitUseCase: DB write completed (forced inactive). Timestamp: ${System.currentTimeMillis()}")
            com.example.core.infrastructure.widget.HabitWidgetSyncUpdater.updateNow(context)
            Pair(ActivationResult.SavedAsInactive(activeCount), forcedInactive)
        } else {
            val finalHabit = updatedHabit.copy(startedAt = System.currentTimeMillis())
            repository.updateHabit(finalHabit)
            android.util.Log.d("HabitWidgetSync", "[LOG] UpdateHabitUseCase: DB write completed (newly activated). Timestamp: ${System.currentTimeMillis()}")
            com.example.core.infrastructure.widget.HabitWidgetSyncUpdater.updateNow(context)
            Pair(ActivationResult.Activated, finalHabit)
        }
    }
}
