package com.example.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PendingOverlayReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return
        android.util.Log.e("ReminderChain", "[PendingOverlayReceiver] onReceive: USER_PRESENT")

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val pendingOverlayStore = PendingOverlayStore(appContext)
                val pending = pendingOverlayStore.consumePending()
                android.util.Log.e("ReminderChain", "[PendingOverlayReceiver] pending overlay found: ${pending != null}")
                if (pending != null) {
                    val serviceIntent = Intent(appContext, HabitOverlayService::class.java).apply {
                        putExtra("HABIT_ID", pending.habitId)
                        putExtra("HABIT_NAME", pending.habitName)
                        putExtra("HABIT_COLOR", pending.habitColor)
                        putExtra("HABIT_DESC", pending.habitDesc)
                    }
                    android.util.Log.e("ReminderChain", "[PendingOverlayReceiver] Starting HabitOverlayService...")
                    ContextCompat.startForegroundService(appContext, serviceIntent)
                    android.util.Log.e("ReminderChain", "[PendingOverlayReceiver] startForegroundService call completed.")
                }
            } catch (e: Exception) {
                android.util.Log.e("ReminderChain", "[PendingOverlayReceiver] CRITICAL: Failed to start foreground service", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
