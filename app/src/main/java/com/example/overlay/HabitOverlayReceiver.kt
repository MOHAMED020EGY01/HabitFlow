package com.example.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class HabitOverlayReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId    = intent.getIntExtra("HABIT_ID", -1)
        val habitName  = intent.getStringExtra("HABIT_NAME") ?: "Habit"
        val habitColor = intent.getStringExtra("HABIT_COLOR") ?: "#7C4DFF"
        val habitDesc  = intent.getStringExtra("HABIT_DESC") ?: ""
        
        android.util.Log.e("ReminderChain", "[HabitOverlayReceiver] onReceive for '$habitName'. Enqueueing...")

        val request = OverlayRequest(
            habitId = habitId,
            habitName = habitName,
            habitColor = habitColor,
            habitDesc = habitDesc,
            triggeredAtEpochMillis = System.currentTimeMillis()
        )
        
        OverlayQueueManager.getInstance(context).enqueue(request)
    }

}
