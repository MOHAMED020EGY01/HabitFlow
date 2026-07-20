package com.example.feature.habit.domain

import android.content.Context
import com.example.core.model.domain.Habit
import com.example.core.repository.HabitRepository

class DeleteHabitUseCase(
    private val repository: HabitRepository,
    private val context: Context
) {
    suspend operator fun invoke(habit: Habit) {
        android.util.Log.d("HabitWidgetSync", "[LOG] DeleteHabitUseCase: Preparing to delete habit. ID: ${habit.id}. Timestamp: ${System.currentTimeMillis()}")
        repository.deleteHabit(habit)
        android.util.Log.d("HabitWidgetSync", "[LOG] DeleteHabitUseCase: DB write completed (delete). Timestamp: ${System.currentTimeMillis()}")
        com.example.core.infrastructure.widget.HabitWidgetSyncUpdater.updateNowForced(context.applicationContext)
    }
}
