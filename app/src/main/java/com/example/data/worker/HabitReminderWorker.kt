package com.example.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.HabitApplication
import com.example.MainActivity
import com.example.domain.model.Habit
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class HabitReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val habitName = inputData.getString("HABIT_NAME") ?: "Build your habit"
        val habitId = inputData.getInt("HABIT_ID", 0)
        val activeDaysCsv = inputData.getString("ACTIVE_DAYS")
        val scheduledTimeStr = inputData.getString("SCHEDULED_TIME") ?: "unknown"

        android.util.Log.e("ReminderChain", "[HabitReminderWorker] doWork started for '$habitName' (ID: $habitId). Scheduled for: $scheduledTimeStr. Actual time: ${java.time.LocalDateTime.now()}")

        // ── Respect activeDays: skip if today is not an active day ───────
        val todayDayName = java.time.LocalDate.now().dayOfWeek.name
        if (activeDaysCsv != null && activeDaysCsv.isNotBlank()) {
            val activeDays = activeDaysCsv.split(",").map { it.trim() }.toSet()
            if (todayDayName !in activeDays) {
                android.util.Log.d("HabitReminder",
                    "Skipping reminder for '$habitName' — $todayDayName not in active days")
                return Result.success()
            }
        } else {
            // Fallback: If for some reason activeDaysCsv is missing, 
            // we should technically check the DB to be safe (Step 4 defense in depth)
            try {
                val db = com.example.data.local.database.HabitDatabase.getDatabase(context)
                val habit = db.habitDao().getHabitById(habitId)
                if (habit != null) {
                    val activeDays = habit.activeDays.toSet()
                    if (todayDayName !in activeDays) {
                        android.util.Log.d("HabitReminder",
                            "Skipping reminder (DB fallback) for '$habitName' — $todayDayName not in active days")
                        return Result.success()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // ── Trigger Speech Reminder ───────────────────────────────────
        val app = context.applicationContext as HabitApplication
        try {
            val db = com.example.data.local.database.HabitDatabase.getDatabase(context)
            val habit = db.habitDao().getHabitById(habitId)
            if (habit != null) {
                app.reminderSpeechController.speakHabitReminder(habit.toDomain())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // ─────────────────────────────────────────────────────────────

        // ── Follow Reminder Flow: Try Overlay Else Notification ───────
        val canDrawOverlays = android.provider.Settings.canDrawOverlays(context)
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
        val isLocked = keyguardManager.isKeyguardLocked

        // If overlay is available and phone not locked, let HabitOverlayWorker handle it.
        // Otherwise, show notification.
        if (canDrawOverlays && !isLocked) {
            android.util.Log.d("ReminderChain", "[HabitReminderWorker] Overlay is available, skipping notification as per flow.")
        } else {
            showNotification(habitId, habitName)
        }

        com.example.widget.HabitWidgetSyncUpdater.updateNowForced(context)
        return Result.success()
    }

    private fun showNotification(habitId: Int, habitName: String) {
        val areNotificationsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        android.util.Log.e("ReminderChain", "[HabitReminderWorker] showNotification for '$habitName'. areNotificationsEnabled: $areNotificationsEnabled")
        
        val channelId = "habit_reminder_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(com.example.R.string.channel_habit_reminders),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(com.example.R.string.channel_habit_reminders)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("HABIT_ID", habitId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(habitName)
            .setContentText(context.getString(com.example.R.string.notification_time_for_habit, habitName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(habitId, notification)

        // Log notification to history
        val app = context.applicationContext as HabitApplication
        app.applicationScope.launch {
            app.repository.insertNotification(
                com.example.domain.model.HabitNotification(
                    title = habitName,
                    body = context.getString(com.example.R.string.notification_time_for_habit, habitName),
                    timestamp = System.currentTimeMillis(),
                    type = "REMINDER"
                )
            )
        }
    }

    companion object {
        fun scheduleHabitReminders(context: Context, habit: Habit) {
            val workManager = WorkManager.getInstance(context)
            // Cancel old work for this habit
            workManager.cancelAllWorkByTag("habit_${habit.id}")
            workManager.cancelAllWorkByTag("overlay_${habit.id}")

            if (!habit.isActive) return

            habit.reminderTimes.forEach { time ->
                try {
                    val parts = time.split(":")
                    if (parts.size == 2) {
                        val hour = parts[0].toIntOrNull() ?: return@forEach
                        val minute = parts[1].toIntOrNull() ?: return@forEach

                        val now = LocalDateTime.now()
                        var scheduledTime = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                        if (scheduledTime.isBefore(now)) {
                            scheduledTime = scheduledTime.plusDays(1)
                        }

                        val delay = Duration.between(now, scheduledTime).toMillis()
                        android.util.Log.e("ReminderChain", "[HabitReminderWorker] scheduleHabitReminders: habit='${habit.name}', time='$time', targetScheduledTime=$scheduledTime, delayMillis=$delay")

                        val activeDaysCsv = habit.activeDays.joinToString(",") { it.name }

                        val request = PeriodicWorkRequestBuilder<HabitReminderWorker>(1, TimeUnit.DAYS)
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .setInputData(
                                workDataOf(
                                    "HABIT_NAME" to habit.name,
                                    "HABIT_ID" to habit.id,
                                    "ACTIVE_DAYS" to activeDaysCsv,
                                    "SCHEDULED_TIME" to scheduledTime.toString()
                                )
                            )
                            .addTag("habit_${habit.id}")
                            .build()

                        workManager.enqueueUniquePeriodicWork(
                            "habit_${habit.id}_$time",
                            ExistingPeriodicWorkPolicy.REPLACE,
                            request
                        )

                        // ── Overlay Worker (also receives activeDays) ────────────────
                        android.util.Log.e("ReminderChain", "[HabitOverlayWorker] Scheduling overlay for '${habit.name}' at $time, targetScheduledTime=$scheduledTime, delayMillis=$delay")
                        val overlayRequest = PeriodicWorkRequestBuilder<HabitOverlayWorker>(1, TimeUnit.DAYS)
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .setInputData(
                                workDataOf(
                                    "HABIT_ID"    to habit.id,
                                    "HABIT_NAME"  to habit.name,
                                    "HABIT_COLOR" to habit.colorHex,
                                    "HABIT_DESC"  to habit.description,
                                    "ACTIVE_DAYS" to activeDaysCsv,
                                    "SCHEDULED_TIME" to scheduledTime.toString()
                                )
                            )
                            .addTag("overlay_${habit.id}")
                            .build()

                        workManager.enqueueUniquePeriodicWork(
                            "overlay_${habit.id}_$time",
                            ExistingPeriodicWorkPolicy.REPLACE,
                            overlayRequest
                        )
                        // ─────────────────────────────────────────────────────────────
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun cancelHabitReminders(context: Context, habitId: Int) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag("habit_$habitId")
            workManager.cancelAllWorkByTag("overlay_$habitId")
        }
    }
}
