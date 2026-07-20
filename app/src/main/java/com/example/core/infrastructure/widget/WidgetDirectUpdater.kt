package com.example.core.infrastructure.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.runComposition
import com.example.app.HabitApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object WidgetDirectUpdater {
    private val updateMutex = Mutex()

    @OptIn(ExperimentalGlanceApi::class)
    suspend fun pushDirectUpdate(context: Context, widget: GlanceAppWidget, glanceId: GlanceId) {
        val appWidgetId = try {
            GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        } catch (_: Exception) {
            -1
        }
        
        android.util.Log.d("HabitWidgetSync", "[LOG] pushDirectUpdate for widget ${widget::class.java.simpleName}, appWidgetId=$appWidgetId. Timestamp: ${System.currentTimeMillis()}")
        
        if (appWidgetId == -1) return
        
        // Ensure only one update runs for this widget at a time across the entire app.
        // This is critical to prevent Glance session conflicts.
        updateMutex.withLock {
            try {
                android.util.Log.d("HabitWidgetPerf", "[LOG] Direct update starting for appWidgetId=$appWidgetId. Timestamp: ${System.currentTimeMillis()}")
                
                // PART 160: Defensive composition with retry logic to fix ConcurrentModificationException.
                // We use Dispatchers.Main.immediate to ensure composition happens in a stable environment.
                val remoteViews = withContext(Dispatchers.Main.immediate) {
                    try {
                        widget.runComposition(context, glanceId).first()
                    } catch (_: java.util.ConcurrentModificationException) {
                        Log.w("WidgetDirectUpdater", "Caught ConcurrentModificationException in runComposition for $appWidgetId, retrying after delay...")
                        delay(200) // Give the previous session a moment to settle
                        widget.runComposition(context, glanceId).first()
                    }
                }

                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, remoteViews)
                android.util.Log.d("HabitWidgetPerf", "[LOG] Direct update pushed for appWidgetId=$appWidgetId. Timestamp: ${System.currentTimeMillis()}")
            } catch (e: Exception) {
                Log.e("WidgetDirectUpdater", "Direct update failed for appWidgetId=$appWidgetId", e)
            }
        }
    }
}
