package com.example.core.infrastructure.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.app.MainActivity
import com.example.R

object NotificationHelper {
    const val HABIT_REMINDER_CHANNEL_ID = "habit_reminders_channel"
    const val INACTIVITY_CHANNEL_ID = "habit_inactivity_channel"
    const val BACKGROUND_CHANNEL_ID = "habit_background_keepalive_channel"
    const val OVERLAY_CHANNEL_ID = "habit_overlay_silent_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(
                    HABIT_REMINDER_CHANNEL_ID,
                    context.getString(R.string.channel_habit_reminders),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.channel_habit_reminders)
                    setSound(null, null)
                    enableVibration(true)
                },
                NotificationChannel(
                    INACTIVITY_CHANNEL_ID,
                    context.getString(R.string.channel_inactivity_alerts),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.channel_inactivity_alerts)
                },
                NotificationChannel(
                    BACKGROUND_CHANNEL_ID,
                    "Background Reliability",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "Keeps habit reminders & overlay reliable"
                    setShowBadge(false)
                },
                NotificationChannel(
                    OVERLAY_CHANNEL_ID,
                    context.getString(R.string.channel_overlay),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.channel_overlay)
                    setSound(null, null)
                }
            )
            notificationManager.createNotificationChannels(channels)
        }
    }

    fun buildHabitReminderNotification(
        context: Context,
        habitId: Int,
        habitName: String
    ): Notification {
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

        return NotificationCompat.Builder(context, HABIT_REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(habitName)
            .setContentText(context.getString(R.string.notification_time_for_habit, habitName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSilent(true)
            .build()
    }

    fun buildInactivityNotification(
        context: Context,
        habitId: Int,
        habitName: String
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("HABIT_ID", habitId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId + 100000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "$habitName Paused"
        val body = "Your habit was auto-paused due to 3 missed days. Resume anytime! ☀️"

        return NotificationCompat.Builder(context, INACTIVITY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    fun buildBackgroundServiceNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, BACKGROUND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_habit_notification)
            .setContentTitle("HabitFlow")
            .setContentText("Background reliability active")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    fun buildOverlayServiceNotification(
        context: Context,
        habitName: String
    ): Notification {
        return NotificationCompat.Builder(context, OVERLAY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_habit_notification)
            .setContentTitle(context.getString(R.string.overlay_title))
            .setContentText(context.getString(R.string.notification_time_for_habit, habitName))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
