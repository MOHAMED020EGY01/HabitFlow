package com.example.domain.usecase

import android.content.Context
import com.example.domain.model.Habit
import com.example.domain.repository.HabitRepository

class DeleteHabitUseCase(
    private val repository: HabitRepository,
    private val context: Context
) {
    suspend operator fun invoke(habit: Habit) {
        android.util.Log.d("HabitWidgetSync", "[LOG] DeleteHabitUseCase: Preparing to delete habit. ID: ${habit.id}. Timestamp: ${System.currentTimeMillis()}")
        repository.deleteHabit(habit)
        android.util.Log.d("HabitWidgetSync", "[LOG] DeleteHabitUseCase: DB write completed (delete). Timestamp: ${System.currentTimeMillis()}")
        com.example.widget.HabitWidgetSyncUpdater.updateNowForced(context.applicationContext)
    }
}
