package com.example.domain.usecase

import android.content.Context
import com.example.domain.model.Habit
import com.example.domain.model.HabitLog
import com.example.domain.model.HabitStatus
import com.example.domain.repository.HabitRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

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
            val cycleEnd = Instant.ofEpochMilli(habit.cycleEndDate)
                .atZone(ZoneId.systemDefault()).toLocalDate()

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

            while (!tempDate.isAfter(yesterday) && tempDate.isBefore(cycleEnd)) {
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
                        val newEndDate = currentHabit.cycleEndDate + 86400000L
                        currentHabit = currentHabit.copy(
                            inactiveDaysCount = currentHabit.inactiveDaysCount + 1,
                            cycleEndDate = newEndDate
                        )
                        repository.updateHabit(currentHabit)
                    }
                }
                tempDate = tempDate.plusDays(1)
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

        val logs = repository.getLogsForHabitSync(habit.id)
        val lastThreeMiss = (1..3).all { i ->
            logs.find { it.logDate == today.minusDays(i.toLong()).toString() }?.state == "MISS"
        }

        if (lastThreeMiss) {
            val updated = habit.copy(
                status = HabitStatus.INACTIVE, 
                isActive = false,
                inactiveSinceTimestamp = System.currentTimeMillis()
            )
            repository.updateHabit(updated)
            sendInactivityNotification(context, repository, updated)
        }
    }

    private suspend fun checkAndCompleteCycle(
        context: Context,
        repository: HabitRepository,
        habit: Habit,
        today: LocalDate,
        cycleStart: LocalDate,
        cycleEnd: LocalDate
    ) {
        if (habit.status != HabitStatus.ACTIVE) return

        // ── Completion Condition ──────────────────────────────────────────
        // A cycle is eligible for completion if:
        // 1. Today is strictly AFTER the cycle end (traditional rollover logic).
        // 2. Today IS the cycle end day AND the user has marked it done.
        // ──────────────────────────────────────────────────────────────────
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

        // Store cycle history with simple string format (no Gson)
        val logsSnapshot = updatedLogs.joinToString(";") { "${it.logDate}:${it.state}" }
        val history = com.example.domain.model.HabitCycleHistory(
            habitId = updated.id,
            cycleStartDate = updated.cycleStartDate,
            cycleEndDate = updated.cycleEndDate,
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
        val cycleEnd = Instant.ofEpochMilli(habit.cycleEndDate)
            .atZone(ZoneId.systemDefault()).toLocalDate()

        checkAndCompleteCycle(context, repository, habit, today, cycleStart, cycleEnd)
    }

    private suspend fun sendInactivityNotification(
        context: Context,
        repository: HabitRepository,
        habit: Habit
    ) {
        val channelId = "habit_inactivity_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(com.example.R.string.channel_inactivity_alerts),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(com.example.R.string.channel_inactivity_alerts)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("HABIT_ID", habit.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            habit.id + 100000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "${habit.name} Paused"
        val body = "Your habit was auto-paused due to 3 missed days. Resume anytime! ☀️"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(habit.id + 100000, notification)

        // Log notification
        repository.insertNotification(
            com.example.domain.model.HabitNotification(
                title = title,
                body = body,
                timestamp = System.currentTimeMillis(),
                type = "PAUSE"
            )
        )
    }
}
