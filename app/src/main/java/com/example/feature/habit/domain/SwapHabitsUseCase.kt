package com.example.feature.habit.domain

import com.example.core.model.domain.Habit
import com.example.core.model.domain.HabitStatus
import com.example.core.repository.HabitRepository
import com.example.core.infrastructure.worker.HabitReminderWorker
import android.content.Context

class SwapHabitsUseCase(
    private val repository: HabitRepository,
    private val context: Context
) {
    suspend operator fun invoke(toActivateId: Int, toDeactivateId: Int) {
        // 1. Deactivate one
        repository.setHabitActive(
            toDeactivateId,
            isActive = false,
            startedAt = null,
            status = HabitStatus.INACTIVE,
            inactiveSince = System.currentTimeMillis()
        )
        HabitReminderWorker.cancelHabitReminders(context, toDeactivateId)

        // 2. Activate the other (with Restart logic if needed)
        val habitToActivate = repository.getHabitById(toActivateId)
        if (habitToActivate != null) {
            val isRestart = habitToActivate.status == HabitStatus.COMPLETE || 
                            habitToActivate.status == HabitStatus.FAILURE

            if (isRestart) {
                val todayMillis = System.currentTimeMillis()
                val newEndDate = todayMillis + (habitToActivate.durationDays * 24L * 60L * 60L * 1000L)
                val restartedHabit = habitToActivate.copy(
                    status = HabitStatus.ACTIVE,
                    isActive = true,
                    cycleStartDate = todayMillis,
                    cycleEndDate = newEndDate,
                    inactiveDaysCount = 0,
                    inactiveSinceTimestamp = null,
                    startedAt = todayMillis
                )
                repository.deleteLogsForHabit(toActivateId)
                repository.updateHabit(restartedHabit)
                HabitReminderWorker.scheduleHabitReminders(context, restartedHabit)
            } else {
                repository.setHabitActive(
                    toActivateId,
                    isActive = true,
                    startedAt = System.currentTimeMillis(),
                    status = HabitStatus.ACTIVE,
                    inactiveSince = null
                )
                val updated = repository.getHabitById(toActivateId)
                if (updated != null) {
                    HabitReminderWorker.scheduleHabitReminders(context, updated)
                }
            }
        }

        com.example.core.infrastructure.widget.HabitWidgetSyncUpdater.updateNow(context)
    }
}
