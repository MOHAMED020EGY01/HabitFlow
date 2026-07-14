package com.example.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Worker responsible for refreshing the contents of the Glance widgets.
 */
class HabitWidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? com.example.HabitApplication
            if (app != null) {
                try {
                    com.example.domain.usecase.HabitStatusManager.performDailyRollover(applicationContext, app.repository)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            HabitWidgetSyncUpdater.refreshAllPlacedWidgets(applicationContext)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val DAILY_WORK_NAME = "habit_widget_daily_refresh"
        private const val IMMEDIATE_WORK_NAME = "habit_widget_immediate_refresh"

        /** Call this once at app startup (e.g. in Application.onCreate). */
        fun scheduleDaily(context: Context) {
            try {
                val now = LocalDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
                val delay = Duration.between(now, nextMidnight).toMillis()

                val request = PeriodicWorkRequestBuilder<HabitWidgetUpdateWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    DAILY_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /** Call this after any habit data mutation. */
        fun scheduleImmediate(context: Context) {
            try {
                val request = OneTimeWorkRequestBuilder<HabitWidgetUpdateWorker>().build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    IMMEDIATE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
