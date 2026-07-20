package com.example.core.infrastructure.overlay

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import java.util.ArrayDeque
import java.time.LocalDate

data class OverlayRequest(
    val habitId: Int,
    val habitName: String,
    val habitColor: String,
    val habitDesc: String,
    val triggeredAtEpochMillis: Long,
    val isCatchUp: Boolean = false
)

class OverlayQueueManager private constructor(private val context: Context) {
    private val queue = ArrayDeque<OverlayRequest>()
    private var isShowing = false
    private val STALENESS_MS = 15 * 60 * 1000L // 15 minutes

    // Track which habits had their overlay shown today to avoid duplicates
    private val alreadyShownToday = mutableMapOf<Int, String>()

    companion object {
        @Volatile
        private var instance: OverlayQueueManager? = null

        fun getInstance(context: Context): OverlayQueueManager {
            return instance ?: synchronized(this) {
                instance ?: OverlayQueueManager(context.applicationContext).also { instance = it }
            }
        }
    }

    @Synchronized
    fun enqueue(request: OverlayRequest) {
        val today = LocalDate.now().toString()
        // Daily cleanup
        if (alreadyShownToday.isNotEmpty() && alreadyShownToday.values.first() != today) {
            alreadyShownToday.clear()
        }

        if (alreadyShownToday[request.habitId] == today && !request.isCatchUp) {
            android.util.Log.d("OverlayQueue", "Skipping duplicate overlay for habit ${request.habitId} today")
            return
        }

        // Replace duplicate habitId if already in queue
        queue.removeIf { it.habitId == request.habitId }
        queue.addLast(request)
        android.util.Log.d("OverlayQueue", "Enqueued request for '${request.habitName}'. Queue size: ${queue.size}, isCatchUp: ${request.isCatchUp}")
        processQueue()
    }

    @Synchronized
    fun onOverlayDismissed() {
        android.util.Log.d("OverlayQueue", "Current overlay dismissed. Processing next in queue.")
        isShowing = false
        processQueue()
    }

    @Synchronized
    private fun processQueue() {
        if (isShowing) {
            android.util.Log.d("OverlayQueue", "Overlay already showing, waiting...")
            return
        }
        
        val now = System.currentTimeMillis()
        val today = LocalDate.now().toString()

        while (queue.isNotEmpty()) {
            val request = queue.removeFirst()
            
            // Re-check if already shown (case where a catch-up enqueued while a normal one was showing)
            if (alreadyShownToday[request.habitId] == today) {
                continue
            }

            // Staleness check: 15 mins for normal, same-day for catch-up
            if (request.isCatchUp) {
                // For catch-up, we just ensure it's from today (triggeredAtEpochMillis is when reminder was scheduled)
                val triggerDate = java.time.Instant.ofEpochMilli(request.triggeredAtEpochMillis)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
                if (triggerDate != today) {
                    android.util.Log.w("OverlayQueue", "Dropping stale CATCH-UP request for '${request.habitName}' from yesterday")
                    continue
                }
            } else {
                if (now - request.triggeredAtEpochMillis > STALENESS_MS) {
                    android.util.Log.w("OverlayQueue", "Dropping stale overlay request for '${request.habitName}' (triggered ${ (now - request.triggeredAtEpochMillis)/1000 }s ago)")
                    continue
                }
            }

            android.util.Log.d("OverlayQueue", "Showing overlay for '${request.habitName}' from queue.")
            alreadyShownToday[request.habitId] = today
            showOverlay(request)
            isShowing = true
            return
        }
        android.util.Log.d("OverlayQueue", "Queue empty.")
    }

    private fun showOverlay(request: OverlayRequest) {
        val serviceIntent = Intent(context, HabitOverlayService::class.java).apply {
            putExtra("HABIT_ID",    request.habitId)
            putExtra("HABIT_NAME",  request.habitName)
            putExtra("HABIT_COLOR", request.habitColor)
            putExtra("HABIT_DESC",  request.habitDesc)
        }
        try {
            // Ensure the persistent background service is running to avoid startForeground exceptions
            com.example.core.infrastructure.service.HabitBackgroundService.start(context)
            
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            android.util.Log.e("OverlayQueue", "Failed to start HabitOverlayService", e)
            isShowing = false
            processQueue()
        }
    }
}
