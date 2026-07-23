package com.example.core.domain.usecase

import android.app.NotificationManager
import android.content.Context
import com.example.core.infrastructure.notification.NotificationHelper
import com.example.core.model.domain.Habit
import com.example.core.model.domain.HabitLog
import com.example.core.model.domain.HabitStatus
import com.example.core.repository.HabitRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId

object HabitStatusManager {

    /**
     * Fills in missing MISS/INACTIVE_SKIPPED logs for yesterday and earlier,
     * using bulk insert and a 30-day lookback limit. Does NOT run on every
     * app startup — called by [DailyRolloverWorker] once per day.
     */
    suspend fun performDailyRollover(context: Context, repository: HabitRepository) {
        val habits = repository.getAllHabitsSync()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        for (habit in habits) {
            if (habit.status != HabitStatus.ACTIVE && habit.status != HabitStatus.INACTIVE) continue

            val cycleStart = Instant.ofEpochMilli(habit.cycleStartDate)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            val cycleEnd = habit.cycleEndDate?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            }

            if (cycleStart.isAfter(yesterday)) continue

            // Limit lookback to 30 days to prevent infinite loops on very old habits
            val effectiveStart = maxOf(cycleStart, today.minusDays(30))

            // Batch-load existing logs to avoid N+1 queries
            val existingLogs = repository.getLogsForHabitSync(habit.id)
            val existingDates = existingLogs.map { it.logDate }.toSet()

            // Build bulk insert list
            val logsToInsert = mutableListOf<HabitLog>()
            var currentHabit = habit
            var tempDate = effectiveStart

            while (!tempDate.isAfter(yesterday) && (cycleEnd == null || tempDate.isBefore(cycleEnd))) {
                val dateStr = tempDate.toString()

                // ── activeDays check for ACTIVE habits ────────────────────
                // Skip non-scheduled days for ACTIVE habits only (don't create MISS logs).
                // For INACTIVE habits, ALL days count for cycle extension regardless.
                val dayOfWeek = tempDate.dayOfWeek
                if (currentHabit.status == HabitStatus.ACTIVE && dayOfWeek !in currentHabit.activeDays) {
                    tempDate = tempDate.plusDays(1)
                    continue
                }

                if (dateStr !in existingDates) {
                    if (currentHabit.status == HabitStatus.ACTIVE) {
                        logsToInsert.add(HabitLog(
                            habitId = currentHabit.id,
                            logDate = dateStr,
                            completed = false,
                            state = "MISS"
                        ))
                    } else if (currentHabit.status == HabitStatus.INACTIVE) {
                        logsToInsert.add(HabitLog(
                            habitId = currentHabit.id,
                            logDate = dateStr,
                            completed = false,
                            state = "INACTIVE_SKIPPED"
                        ))
                        val newEndDate = currentHabit.cycleEndDate?.let { it + 86400000L }
                        currentHabit = currentHabit.copy(
                            inactiveDaysCount = currentHabit.inactiveDaysCount + 1,
                            cycleEndDate = newEndDate
                        )
                    }
                }
                tempDate = tempDate.plusDays(1)
            }

            // Fix Issue #3: Update habit once after the loop instead of inside it
            if (currentHabit != habit) {
                repository.updateHabit(currentHabit)
            }

            // Bulk insert all missing logs at once (single transaction)
            if (logsToInsert.isNotEmpty()) {
                repository.insertLogsBulk(logsToInsert)
            }

            // Check for 3 consecutive misses (last 3 days only)
            checkAndAutoPause(context, repository, currentHabit, today)

            // Check cycle completion
            checkAndCompleteCycle(context, repository, currentHabit, today, cycleStart, cycleEnd)
        }
    }

    private suspend fun checkAndAutoPause(
        context: Context,
        repository: HabitRepository,
        habit: Habit,
        today: LocalDate
    ) {
        if (habit.status != HabitStatus.ACTIVE) return

        // Fix Issue #4: Evaluate last 3 scheduled occurrences instead of consecutive calendar days
        val logs = repository.getLogsForHabitSync(habit.id)
            .filter { 
                try {
                    LocalDate.parse(it.logDate).isBefore(today)
                } catch (e: Exception) {
                    false
                }
            }
            .sortedByDescending { it.logDate }
        
        val lastThreeMiss = logs.take(3).let { taken ->
            taken.size == 3 && taken.all { it.state == "MISS" }
        }

        if (lastThreeMiss) {
            val updated = habit.copy(
                status = HabitStatus.INACTIVE, 
                isActive = false,
                inactiveSinceTimestamp = System.currentTimeMillis()
            )
            repository.updateHabit(updated)
            // CRITICAL: Cancel reminders when auto-paused
            com.example.core.infrastructure.worker.HabitReminderWorker.cancelHabitReminders(context, updated.id)
            sendInactivityNotification(context, repository, updated)
        }
    }

    private suspend fun checkAndCompleteCycle(
        context: Context,
        repository: HabitRepository,
        habit: Habit,
        today: LocalDate,
        cycleStart: LocalDate,
        cycleEnd: LocalDate?
    ) {
        if (habit.status != HabitStatus.ACTIVE) return

        if (habit.durationType == com.example.core.model.domain.HabitDurationType.OCCURRENCE) {
            val target = habit.targetOccurrenceCount ?: 0
            val completedCount = repository.getCompletedCountForCycle(habit.id, cycleStart.toString())
            
            if (completedCount >= target) {
                val updated = habit.copy(
                    status = HabitStatus.COMPLETE,
                    isActive = false
                )
                repository.updateHabit(updated)
                // CRITICAL: Cancel reminders when completed
                com.example.core.infrastructure.worker.HabitReminderWorker.cancelHabitReminders(context, updated.id)

                val updatedLogs = repository.getLogsForHabitSync(habit.id)
                    .filter { log ->
                        val d = LocalDate.parse(log.logDate)
                        !d.isBefore(cycleStart)
                    }
                val logsSnapshot = updatedLogs.joinToString(";") { "${it.logDate}:${it.state}" }
                val history = com.example.core.model.domain.HabitCycleHistory(
                    habitId = updated.id,
                    cycleStartDate = updated.cycleStartDate,
                    cycleEndDate = System.currentTimeMillis(), // Current time for occurrence
                    completionPercentage = 100.0,
                    result = HabitStatus.COMPLETE.name,
                    logsSnapshot = logsSnapshot
                )
                repository.insertCycleHistory(history)
            }
            return
        }

        // ── Calendar Completion Condition ──────────────────────────────────────────
        if (cycleEnd == null) return // Should not happen for CALENDAR

        val lastScheduledDay = cycleEnd.minusDays(1)
        val isLastDayOrLater = !today.isBefore(lastScheduledDay)
        
        if (!isLastDayOrLater) return

        val updatedLogs = repository.getLogsForHabitSync(habit.id)
            .filter { log ->
                val d = LocalDate.parse(log.logDate)
                !d.isBefore(cycleStart) && d.isBefore(cycleEnd)
            }

        // Check if today's log is done if today is the last day
        val todayStr = today.toString()
        val isDoneOnLastDay = updatedLogs.any { it.logDate == todayStr && (it.completed || it.state == "DONE") }
        
        // If it's exactly the last day, only complete if it's actually marked DONE.
        // This prevents premature FAILURE status on the last day if the user hasn't
        // finished their day yet.
        if (today == lastScheduledDay && !isDoneOnLastDay) return

        val doneCount = updatedLogs.count { it.state == "DONE" || it.completed }
        val missCount = updatedLogs.count { it.state == "MISS" }
        val totalCount = doneCount + missCount

        val completionPercentage = if (totalCount > 0) {
            (doneCount.toDouble() / totalCount.toDouble()) * 100.0
        } else 0.0

        val finalStatus = if (completionPercentage >= 90.0) HabitStatus.COMPLETE else HabitStatus.FAILURE

        val updated = habit.copy(
            status = finalStatus,
            isActive = false
        )
        repository.updateHabit(updated)
        // CRITICAL: Cancel reminders
        com.example.core.infrastructure.worker.HabitReminderWorker.cancelHabitReminders(context, updated.id)

        // Store cycle history with simple string format (no Gson)
        val logsSnapshot = updatedLogs.joinToString(";") { "${it.logDate}:${it.state}" }
        val history = com.example.core.model.domain.HabitCycleHistory(
            habitId = updated.id,
            cycleStartDate = updated.cycleStartDate,
            cycleEndDate = updated.cycleEndDate ?: updated.cycleStartDate,
            completionPercentage = completionPercentage,
            result = finalStatus.name,
            logsSnapshot = logsSnapshot
        )
        repository.insertCycleHistory(history)
    }

    /**
     * Public entry point to check completion for a single habit.
     * Useful for real-time status updates after user actions.
     */
    suspend fun checkHabitCompletion(context: Context, repository: HabitRepository, habitId: Int) {
        val habit = repository.getHabitById(habitId) ?: return
        val today = LocalDate.now()
        val cycleStart = Instant.ofEpochMilli(habit.cycleStartDate)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val cycleEnd = habit.cycleEndDate?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }

        checkAndCompleteCycle(context, repository, habit, today, cycleStart, cycleEnd)
    }

    private suspend fun sendInactivityNotification(
        context: Context,
        repository: HabitRepository,
        habit: Habit
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationHelper.createNotificationChannels(context)
        val notification = NotificationHelper.buildInactivityNotification(context, habit.id, habit.name)

        notificationManager.notify(habit.id + 100000, notification)

        // Log notification
        repository.insertNotification(
            com.example.core.model.domain.HabitNotification(
                title = "${habit.name} Paused",
                body = "Your habit was auto-paused due to 3 missed days. Resume anytime! ☀️",
                timestamp = System.currentTimeMillis(),
                type = "PAUSE"
            )
        )
    }
}
