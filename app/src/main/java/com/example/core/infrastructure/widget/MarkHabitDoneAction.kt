package com.example.core.infrastructure.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import com.example.app.HabitApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MarkHabitDoneAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[habitIdKey] ?: return
        val app = context.applicationContext as HabitApplication
        
        // Fix: Ensure application is initialized before accessing repository
        try {
            app.ensureInitialized()
        } catch (_: Exception) {
            return
        }
        
        android.util.Log.d("HabitWidgetPerf", "[LOG] MarkHabitDoneAction started for habit $habitId. Timestamp: ${System.currentTimeMillis()}")
        
        // 0. Verify scheduling before DB write
        val habit = withContext(Dispatchers.IO) { app.repository.getHabitById(habitId) }
        if (habit != null) {
            val today = java.time.LocalDate.now()
            if (!habit.isScheduledOn(today)) {
                android.util.Log.w("HabitWidgetPerf", "[LOG] MarkHabitDoneAction: Attempted to mark done on non-active day for habit $habitId")
                return
            }
        } else {
            return
        }

        // 1. Direct DB write (awaited)
        val repository = HabitWidgetRepository(app.repository)
        withContext(Dispatchers.IO) {
            repository.markHabitDoneToday(habitId)
            
            // Check for instant cycle completion if this was the last day
            com.example.core.domain.usecase.HabitStatusManager.checkHabitCompletion(
                app.applicationContext,
                app.repository,
                habitId
            )
        }
        android.util.Log.d("HabitWidgetPerf", "[LOG] MarkHabitDoneAction DB write completed. Timestamp: ${System.currentTimeMillis()}")

        // 2. Direct widget update (Bypassing Glance locking mechanism)
        try {
            val widget: GlanceAppWidget = AllHabitsWidget()
            WidgetDirectUpdater.pushDirectUpdate(context, widget, glanceId)
            android.util.Log.d("HabitWidgetPerf", "[LOG] MarkHabitDoneAction direct update pushed. Timestamp: ${System.currentTimeMillis()}")
        } catch (e: Exception) {
            android.util.Log.e("HabitWidget", "Failed to push direct update in MarkHabitDoneAction", e)
        }

        // 3. Trigger full sync in background (reconciliation)
        HabitWidgetSyncUpdater.updateNow(context.applicationContext)
    }

    companion object {
        val habitIdKey = ActionParameters.Key<Int>("HABIT_ID")
    }
}
