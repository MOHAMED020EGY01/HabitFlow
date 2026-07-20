package com.example.feature.habit.domain

import android.content.Context
import com.example.core.model.domain.ActivationResult
import com.example.core.model.domain.MAX_ACTIVE_HABITS
import com.example.core.model.domain.Habit
import com.example.core.repository.HabitRepository

class AddHabitUseCase(
    private val repository: HabitRepository,
    private val context: Context
) {
    suspend operator fun invoke(habit: Habit): Pair<ActivationResult, Habit> {
        android.util.Log.d("HabitWidgetSync", "[LOG] AddHabitUseCase: Preparing to insert habit. Timestamp: ${System.currentTimeMillis()}")
        // The habit isn't meant to be active from the start → save as-is
        if (!habit.isActive) {
            val newId = repository.insertHabit(habit).toInt()
            val finalHabit = habit.copy(id = newId)
            android.util.Log.d("HabitWidgetSync", "[LOG] AddHabitUseCase: DB write completed (inactive habit). Timestamp: ${System.currentTimeMillis()}")
            com.example.core.infrastructure.widget.HabitWidgetSyncUpdater.updateNow(context)
            return Pair(ActivationResult.NotApplicable, finalHabit)
        }

        val activeCount = repository.getActiveHabitsCount()

        return if (activeCount >= MAX_ACTIVE_HABITS) {
            // Limit reached → save it, but force it to inactive
            val forcedInactive = habit.copy(isActive = false, startedAt = null)
            val newId = repository.insertHabit(forcedInactive).toInt()
            val finalHabit = forcedInactive.copy(id = newId)
            android.util.Log.d("HabitWidgetSync", "[LOG] AddHabitUseCase: DB write completed (forced inactive habit). Timestamp: ${System.currentTimeMillis()}")
            com.example.core.infrastructure.widget.HabitWidgetSyncUpdater.updateNow(context)
            Pair(ActivationResult.SavedAsInactive(activeCount), finalHabit)
        } else {
            val finalHabit = habit.copy(startedAt = habit.startedAt ?: System.currentTimeMillis())
            val newId = repository.insertHabit(finalHabit).toInt()
            android.util.Log.d("HabitWidgetSync", "[LOG] AddHabitUseCase: DB write completed (active habit). ID: $newId. Timestamp: ${System.currentTimeMillis()}")
            val finalHabitWithId = finalHabit.copy(id = newId)
            com.example.core.infrastructure.widget.HabitWidgetSyncUpdater.updateNow(context)
            Pair(ActivationResult.Activated, finalHabitWithId)
        }
    }
}
