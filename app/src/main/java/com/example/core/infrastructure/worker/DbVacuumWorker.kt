package com.example.core.infrastructure.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.core.database.HabitDatabase
import java.util.concurrent.TimeUnit

class DbVacuumWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("DbVacuumWorker", "Starting scheduled database optimization...")
            val database = HabitDatabase.getDatabase(applicationContext)
            val db = database.openHelper.writableDatabase
            
            // Check the current auto_vacuum setting
            var autoVacuumType = 0
            val cursor = db.query("PRAGMA auto_vacuum")
            if (cursor.moveToFirst()) {
                autoVacuumType = cursor.getInt(0)
            }
            cursor.close()
            
            if (autoVacuumType != 2) { // 2 corresponds to INCREMENTAL
                Log.d("DbVacuumWorker", "auto_vacuum is not set to INCREMENTAL (current: $autoVacuumType). Enabling...")
                db.execSQL("PRAGMA auto_vacuum = INCREMENTAL")
                // A full VACUUM is required once to convert the file structure
                db.execSQL("VACUUM")
                Log.d("DbVacuumWorker", "Database auto_vacuum converted to INCREMENTAL with full VACUUM.")
            } else {
                // Run incremental vacuum to reclaim space on deleted rows
                db.execSQL("PRAGMA incremental_vacuum(500)")
                Log.d("DbVacuumWorker", "PRAGMA incremental_vacuum completed successfully.")
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("DbVacuumWorker", "Error running database vacuum", e)
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "database_vacuum_work"

        fun schedule(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiresDeviceIdle(true)
                    .setRequiresCharging(true)
                    .build()

                val request = PeriodicWorkRequestBuilder<DbVacuumWorker>(7, TimeUnit.DAYS)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                Log.d("DbVacuumWorker", "DbVacuumWorker scheduled successfully.")
            } catch (e: Exception) {
                Log.e("DbVacuumWorker", "Failed to schedule DbVacuumWorker", e)
            }
        }
    }
}
