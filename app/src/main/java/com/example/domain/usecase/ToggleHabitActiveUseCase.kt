package com.example.domain.usecase

import com.example.domain.model.ActivationResult
import com.example.domain.model.MAX_ACTIVE_HABITS
import com.example.domain.repository.HabitRepository

class ToggleHabitActiveUseCase(
    private val repository: HabitRepository,
    private val context: android.content.Context
) {
    suspend operator fun invoke(habitId: Int, makeActive: Boolean): ActivationResult {
        if (!makeActive) {
            repository.setHabitActive(
                habitId, 
                isActive = false, 
                startedAt = null, 
                status = com.example.domain.model.HabitStatus.INACTIVE,
                inactiveSince = System.currentTimeMillis()
            )
            com.example.widget.HabitWidgetSyncUpdater.updateNow(context)
            return ActivationResult.NotApplicable
        }

        val activeCount = repository.getActiveHabitsCount()

        return if (activeCount >= MAX_ACTIVE_HABITS) {
            ActivationResult.SavedAsInactive(activeCount)
        } else {
            repository.setHabitActive(
                habitId, 
                isActive = true, 
                startedAt = System.currentTimeMillis(),
                status = com.example.domain.model.HabitStatus.ACTIVE,
                inactiveSince = null
            )
            com.example.widget.HabitWidgetSyncUpdater.updateNow(context)
            ActivationResult.Activated
        }
    }
}
