package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.HabitApplication
import com.example.R
import com.example.overlay.OverlayQueueManager
import com.example.overlay.OverlayRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Persistent foreground service that keeps habit reminders and overlay reliable.
 * Also handles device unlock catch-up for missed reminders.
 */
class HabitBackgroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                android.util.Log.d("HabitBackgroundService", "Device unlocked, checking for catch-up overlays")
                checkAndCatchUpOverlays()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundWithSilentNotification()
        
        // Register receiver for device unlock
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        registerReceiver(unlockReceiver, filter)
    }

    private fun startForegroundWithSilentNotification() {
        val channelId = "habit_background_keepalive_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Background Reliability",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps habit reminders & overlay reliable"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_habit_notification)
            .setContentTitle("HabitFlow")
            .setContentText("Background reliability active")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()

        startForeground(2001, notification)
        
        android.util.Log.d("HabitBackgroundService", "Persistent foreground service started for reliability")
    }

    private fun checkAndCatchUpOverlays() {
        val app = application as HabitApplication
        serviceScope.launch {
            try {
                val today = LocalDate.now()
                val todayStr = today.toString()
                val now = LocalTime.now()

                // Get active habits for today
                val habitsWithCompletion = app.repository.getActiveHabitsWithCompletion(todayStr).first()
                
                val queueManager = OverlayQueueManager.getInstance(applicationContext)
                val catchUpRequests = mutableListOf<OverlayRequest>()

                habitsWithCompletion
                    .filter { it.habit.isActiveToday() }
                    .filter { !it.isCompletedToday }
                    .forEach { item ->
                        val habit = item.habit
                        // Check if any reminder time has passed today
                        habit.reminderTimes.forEach { timeStr ->
                            try {
                                val parts = timeStr.split(":")
                                val reminderTime = LocalTime.of(parts[0].toInt(), parts[1].toInt())
                                
                                if (reminderTime.isBefore(now)) {
                                    // Missed reminder detected
                                    val triggerEpoch = LocalDateTime.of(today, reminderTime)
                                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    
                                    val request = OverlayRequest(
                                        habitId = habit.id,
                                        habitName = habit.name,
                                        habitColor = habit.colorHex,
                                        habitDesc = habit.description,
                                        triggeredAtEpochMillis = triggerEpoch,
                                        isCatchUp = true
                                    )
                                    catchUpRequests.add(request)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                
                // Enqueue in order of scheduled time
                catchUpRequests.sortBy { it.triggeredAtEpochMillis }
                catchUpRequests.forEach { queueManager.enqueue(it) }

            } catch (e: Exception) {
                android.util.Log.e("HabitBackgroundService", "Error during catch-up check", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterReceiver(unlockReceiver)
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, HabitBackgroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, HabitBackgroundService::class.java))
        }
    }
}
