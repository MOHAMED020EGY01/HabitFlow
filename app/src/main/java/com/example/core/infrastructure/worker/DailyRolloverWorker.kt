package com.example.core.infrastructure.worker

import android.content.Context
import androidx.work.*
import com.example.app.HabitApplication
import com.example.core.domain.usecase.HabitStatusManager
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Runs once per day (at midnight) to fill in MISS logs for any missed days,
 * check for 3-consecutive-miss auto-pause, and detect cycle completion.
 * Removed from app startup — runs via WorkManager only.
 */
class DailyRolloverWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as HabitApplication
            
            // Fix Issue #1: Ensure app is fully initialized before accessing repository
            app.ensureInitialized()

            val today = java.time.LocalDate.now().toString()
            val lastRollover = app.preferencesManager.lastRolloverDateFlow.first()

            if (lastRollover != today) {
                android.util.Log.d("DailyRollover", "Running daily rollover for $today")
                HabitStatusManager.performDailyRollover(applicationContext, app.repository)
                app.preferencesManager.saveLastRolloverDate(today)
                android.util.Log.d("DailyRollover", "Daily rollover completed")
            }
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("DailyRollover", "Rollover failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "daily_rollover"

        fun schedule(context: Context) {
            try {
                val now = LocalDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
                val delay = Duration.between(now, nextMidnight).toMillis()

                val request = PeriodicWorkRequestBuilder<DailyRolloverWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiresBatteryNotLow(true)
                            .build()
                    )
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                android.util.Log.d("DailyRollover", "Scheduled daily rollover at midnight")
            } catch (e: Exception) {
                android.util.Log.e("DailyRollover", "Failed to schedule", e)
            }
        }
    }
}
