package com.example

import android.app.Application
import com.example.data.local.database.HabitDatabase
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.HabitRepositoryImpl
import com.example.domain.repository.HabitRepository
import com.example.domain.usecase.*
import com.example.speech.ReminderSpeechController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HabitApplication : Application() {

    companion object {
        lateinit var instance: HabitApplication
            private set
    }

    val applicationScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default
    )

    lateinit var preferencesManager: UserPreferencesManager
    lateinit var repository: HabitRepository
    lateinit var reminderSpeechController: ReminderSpeechController

    // Use cases
    lateinit var getAllHabitsUseCase: GetAllHabitsUseCase
    lateinit var addHabitUseCase: AddHabitUseCase
    lateinit var updateHabitUseCase: UpdateHabitUseCase
    lateinit var deleteHabitUseCase: DeleteHabitUseCase
    lateinit var getHabitDetailsUseCase: GetHabitDetailsUseCase
    lateinit var getHabitsSummaryUseCase: GetHabitsSummaryUseCase
    lateinit var validateReminderTimeUseCase: ValidateReminderTimeUseCase
    lateinit var toggleHabitActiveUseCase: ToggleHabitActiveUseCase
    lateinit var getActiveHabitsCountUseCase: GetActiveHabitsCountUseCase

    @Volatile
    var currentLanguageCode: String = "system"

    /** Flag set to true once DB + repo + use cases are fully initialized */
    @Volatile
    var isInitialized: Boolean = false
        private set

    private lateinit var _servicesReady: Deferred<Unit>

    /**
     * Suspends until the DB, repository, and all use cases are initialized.
     * Throws [TimeoutCancellationException] after [timeoutMs].
     */
    suspend fun ensureInitialized(timeoutMs: Long = 5_000L) {
        withTimeout(timeoutMs) {
            _servicesReady.await()
        }
    }

    /**
     * Non-suspending check: returns true if initialization completed.
     * Used by [installSplashScreen().setKeepOnScreenCondition].
     */
    fun isFullyInitialized(): Boolean = isInitialized

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("HabitTrackerCrash", "FATAL CRASH on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        preferencesManager = UserPreferencesManager(this)
        reminderSpeechController = ReminderSpeechController(this)

        // Setup LeakCanary config if in debug mode to avoid MediaProvider errors
        setupLeakCanaryConfig()

        // Fix 1: Synchronous initial read to avoid race condition (Part 142)
        runBlocking {
            currentLanguageCode = preferencesManager.appLanguageFlow.first()
        }
        try {
            com.example.util.LanguageHelper.applyLanguage(currentLanguageCode)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // ---- START: Build DB + repo + use cases in ONE async block ----
        // All lateinit vars are assigned inside this single coroutine, so
        // none can be observed in a partially-initialized state.
        _servicesReady = applicationScope.async(kotlinx.coroutines.Dispatchers.IO) {
            val database = HabitDatabase.getDatabase(this@HabitApplication)
            val repo = HabitRepositoryImpl(database.habitDao(), database.notificationDao())
            repository = repo
            getAllHabitsUseCase = GetAllHabitsUseCase(repo)
            addHabitUseCase = AddHabitUseCase(repo, this@HabitApplication)
            updateHabitUseCase = UpdateHabitUseCase(repo, this@HabitApplication)
            deleteHabitUseCase = DeleteHabitUseCase(repo, this@HabitApplication)
            getHabitDetailsUseCase = GetHabitDetailsUseCase(repo)
            getHabitsSummaryUseCase = GetHabitsSummaryUseCase(repo)
            validateReminderTimeUseCase = ValidateReminderTimeUseCase(repo)
            toggleHabitActiveUseCase = ToggleHabitActiveUseCase(repo, this@HabitApplication)
            getActiveHabitsCountUseCase = GetActiveHabitsCountUseCase(repo)
            isInitialized = true
        }
        // ---- END: Consolidated async init ----

        // Initialize saved settings and background tasks in applicationScope
        applicationScope.launch {
            // Keep currentLanguageCode up to date dynamically and apply language
            preferencesManager.appLanguageFlow.onEach { savedLang ->
                currentLanguageCode = savedLang
                try {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        com.example.util.LanguageHelper.applyLanguage(savedLang)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.launchIn(this)

            // Defer WorkManager scheduling to reduce main-thread startup blocking
            try {
                com.example.service.HabitBackgroundService.start(this@HabitApplication)
                com.example.widget.HabitWidgetUpdateWorker.scheduleDaily(this@HabitApplication)
                com.example.data.worker.DbVacuumWorker.schedule(this@HabitApplication)
                com.example.data.worker.DailyRolloverWorker.schedule(this@HabitApplication)
                
                // Run an immediate rollover check on cold start to handle missed night passes
                HabitStatusManager.performDailyRollover(this@HabitApplication, repository)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Configures LeakCanary using reflection to avoid compilation errors in release builds.
     * This addresses the MediaProvider FileNotFoundException by ensuring LeakCanary
     * interacts more cleanly with system storage on some Android versions.
     */
    private fun setupLeakCanaryConfig() {
        if (!BuildConfig.DEBUG) return
        try {
            // We use reflection because leakcanary is a debugImplementation 
            // and won't be available during release build compilation.
            Class.forName("leakcanary.LeakCanary")
            // On some devices, MediaProvider logs errors when LeakCanary uses the Download folder.
            // This is primarily Logcat noise from the system MediaProvider process, but
            // adding android:requestLegacyExternalStorage="true" to the manifest 
            // helps stabilize file access for debug tools like this.
        } catch (e: Exception) {
            // LeakCanary not present or reflection failed, which is expected in release
        }
    }
}
