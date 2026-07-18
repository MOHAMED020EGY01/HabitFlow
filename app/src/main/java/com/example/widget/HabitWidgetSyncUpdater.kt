package com.example.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.HabitApplication
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Updates all 3 Glance widgets with a 3-second debounce.
 * Multiple rapid calls (e.g., toggling habits) result in only ONE update
 * after the last call, preventing battery drain from repeated widget redraws.
 */
object HabitWidgetSyncUpdater {

    private const val DEBOUNCE_MS = 3000L
    private var pendingUpdateJob: Job? = null
    private val syncMutex = Mutex()

    /**
     * Requests a widget update with debounce.
     * If called again within [DEBOUNCE_MS], the previous pending update is cancelled
     * and rescheduled. Only the last call triggers an actual update.
     */
    fun updateNow(context: Context) {
        val appContext = context.applicationContext
        val app = (appContext as? HabitApplication) ?: return

        // Cancel any previously scheduled update
        pendingUpdateJob?.cancel()

        pendingUpdateJob = app.applicationScope.launch(Dispatchers.Default) {
            delay(DEBOUNCE_MS)

            Log.d("HabitWidgetSync", "[LOG] Debounce finished, executing widget updates.")
            refreshAllPlacedWidgets(appContext)
            Log.d("HabitWidgetSync", "[LOG] All widgets update finished.")
        }
    }

    /**
     * Force an immediate widget update, bypassing debounce.
     * Use for explicit user actions that need instant feedback (e.g., marking done from widget).
     * Now a suspend function to allow callers to await completion.
     */
    suspend fun updateNowForced(context: Context) {
        val app = context.applicationContext as? HabitApplication
        if (app != null) {
            try {
                app.ensureInitialized()
            } catch (_: Exception) {
                // If it fails to initialize within timeout, we still try to update,
                // but refreshAllPlacedWidgets will handle the wait/errors.
            }
        }

        android.util.Log.d("HabitWidgetSync", "[LOG] updateNowForced called. Timestamp: ${System.currentTimeMillis()}")
        pendingUpdateJob?.cancel()
        refreshAllPlacedWidgets(context)
        android.util.Log.d("HabitWidgetSync", "[LOG] updateNowForced completed. Timestamp: ${System.currentTimeMillis()}")
    }

    /**
     * Replaces the standard Glance update cycle with a direct-to-AppWidgetManager push.
     * Bypasses the ~45s session lock inherent in Glance's update() methods.
     */
    suspend fun refreshAllPlacedWidgets(context: Context) {
        // Use a Mutex to ensure only one refresh cycle runs at a time.
        // This prevents redundant ID queries and overlapping update jobs.
        syncMutex.withLock {
            val manager = GlanceAppWidgetManager(context)
            android.util.Log.d("HabitWidgetSync", "[LOG] refreshAllPlacedWidgets started. Timestamp: ${System.currentTimeMillis()}")
            
            val app = context.applicationContext as? HabitApplication
            if (app != null) {
                try { app.ensureInitialized() } catch (_: Exception) {}
            }
            
            withContext(Dispatchers.IO) {
                val jobs = mutableListOf<Job>()
                
                // AllHabitsWidget
                try {
                    val allHabitsIds = manager.getGlanceIds(AllHabitsWidget::class.java)
                    android.util.Log.d("HabitWidgetSync", "[LOG] Found ${allHabitsIds.size} instances of AllHabitsWidget.")
                    allHabitsIds.forEach { glanceId ->
                        jobs.add(launch { WidgetDirectUpdater.pushDirectUpdate(context, AllHabitsWidget(), glanceId) })
                    }
                } catch (e: Exception) {
                    Log.e("HabitWidgetSync", "Failed to get IDs for AllHabitsWidget", e)
                }

                // InactiveHabitsWidget
                try {
                    val inactiveHabitsIds = manager.getGlanceIds(InactiveHabitsWidget::class.java)
                    android.util.Log.d("HabitWidgetSync", "[LOG] Found ${inactiveHabitsIds.size} instances of InactiveHabitsWidget.")
                    inactiveHabitsIds.forEach { glanceId ->
                        jobs.add(launch { WidgetDirectUpdater.pushDirectUpdate(context, InactiveHabitsWidget(), glanceId) })
                    }
                } catch (e: Exception) {
                    Log.e("HabitWidgetSync", "Failed to get IDs for InactiveHabitsWidget", e)
                }

                // HabitStatsSummaryWidget
                try {
                    val summaryIds = manager.getGlanceIds(HabitStatsSummaryWidget::class.java)
                    android.util.Log.d("HabitWidgetSync", "[LOG] Found ${summaryIds.size} instances of HabitStatsSummaryWidget.")
                    summaryIds.forEach { glanceId ->
                        jobs.add(launch { WidgetDirectUpdater.pushDirectUpdate(context, HabitStatsSummaryWidget(), glanceId) })
                    }
                } catch (e: Exception) {
                    Log.e("HabitWidgetSync", "Failed to get IDs for HabitStatsSummaryWidget", e)
                }
                
                jobs.joinAll()
                android.util.Log.d("HabitWidgetSync", "[LOG] refreshAllPlacedWidgets finished joining all jobs. Timestamp: ${System.currentTimeMillis()}")
            }
        }
    }

}
