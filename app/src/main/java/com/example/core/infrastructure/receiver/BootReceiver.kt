package com.example.core.infrastructure.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.core.database.HabitDatabase
import com.example.core.model.entity.HabitEntity
import com.example.core.model.mapper.toDomain
import com.example.core.infrastructure.worker.HabitReminderWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            val database = HabitDatabase.getDatabase(context)
            val dao = database.habitDao()
            val appContext = context.applicationContext
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            scope.launch {
                try {
                    // Removed: persistent background service start.
                    // Reminders are handled by WorkManager PeriodicWorkRequest,
                    // which survives reboots. We just re-schedule them here.
                    val habits = dao.getAllHabits().first()
                    habits.forEach { entity ->
                        if (entity.isActive) {
                            HabitReminderWorker.scheduleHabitReminders(appContext, entity.toDomain())
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
