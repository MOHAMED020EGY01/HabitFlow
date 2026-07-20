package com.example.core.infrastructure.worker

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.core.infrastructure.overlay.HabitOverlayReceiver
import com.example.core.infrastructure.overlay.PendingOverlayStore

class HabitOverlayWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val habitId    = inputData.getInt("HABIT_ID", -1)
        val habitName  = inputData.getString("HABIT_NAME")  ?: return Result.failure()
        val habitColor = inputData.getString("HABIT_COLOR") ?: "#7C4DFF"
        val habitDesc  = inputData.getString("HABIT_DESC")  ?: ""
        val activeDaysCsv = inputData.getString("ACTIVE_DAYS")
        val scheduledTimeStr = inputData.getString("SCHEDULED_TIME") ?: "unknown"

        val now = java.time.LocalDateTime.now()
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val isIgnoringBattery = powerManager.isIgnoringBatteryOptimizations(applicationContext.packageName)
        val isPowerSaveMode = powerManager.isPowerSaveMode

        android.util.Log.e("ReminderChain", "[HabitOverlayWorker] doWork started for '$habitName' (ID: $habitId). " +
                "Scheduled: $scheduledTimeStr. Actual: $now. " +
                "BatteryOpt: ignoring=$isIgnoringBattery, powerSave=$isPowerSaveMode")

        // ── Smart Skip: skip if already completed today ───────────────
        val app = applicationContext as com.example.app.HabitApplication
        try {
            app.ensureInitialized()
            if (app.repository.isHabitCompletedToday(habitId)) {
                android.util.Log.d("HabitOverlayWorker", "Skipping overlay for '$habitName' — already completed today")
                return Result.success()
            }
        } catch (e: Exception) {
            android.util.Log.e("HabitOverlayWorker", "Smart skip check failed", e)
        }

        // ── Respect activeDays: skip if today is not an active day ───────
        val todayDayName = java.time.LocalDate.now().dayOfWeek.name
        if (activeDaysCsv != null && activeDaysCsv.isNotBlank()) {
            val activeDays = activeDaysCsv.split(",").map { it.trim() }.toSet()
            if (todayDayName !in activeDays) {
                android.util.Log.e("ReminderChain", "[HabitOverlayWorker] Skipping overlay for '$habitName' — $todayDayName not in active days")
                return Result.success()
            }
        } else {
            // Fallback (Step 4 defense in depth)
            try {
                val db = com.example.core.database.HabitDatabase.getDatabase(applicationContext)
                val habit = db.habitDao().getHabitById(habitId)
                if (habit != null) {
                    val activeDays = habit.activeDays.toSet()
                    if (todayDayName !in activeDays) {
                        android.util.Log.e("ReminderChain", "[HabitOverlayWorker] Skipping overlay (DB fallback) for '$habitName' — $todayDayName not in active days")
                        return Result.success()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Additional Suspect 2: Permission logging
        val canDrawOverlays = Settings.canDrawOverlays(applicationContext)
        android.util.Log.e("ReminderChain", "[HabitOverlayWorker] canDrawOverlays: $canDrawOverlays")
        if (!canDrawOverlays) {
            android.util.Log.e("ReminderChain", "[HabitOverlayWorker] ABORTING: No SYSTEM_ALERT_WINDOW permission")
            return Result.success()
        }

        val keyguardManager = applicationContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val isLocked = keyguardManager.isKeyguardLocked
        android.util.Log.e("ReminderChain", "[HabitOverlayWorker] isLocked: $isLocked")

        if (isLocked) {
            val pendingStore = PendingOverlayStore(applicationContext)
            pendingStore.savePending(habitId, habitName, habitColor, habitDesc)
        } else {
            val intent = Intent(applicationContext, HabitOverlayReceiver::class.java).apply {
                putExtra("HABIT_ID",    habitId)
                putExtra("HABIT_NAME",  habitName)
                putExtra("HABIT_COLOR", habitColor)
                putExtra("HABIT_DESC",  habitDesc)
            }
            applicationContext.sendBroadcast(intent)
        }

        com.example.core.infrastructure.widget.HabitWidgetSyncUpdater.updateNowForced(applicationContext)

        return Result.success()
    }
}

