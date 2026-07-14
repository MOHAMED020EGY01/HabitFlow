package com.example.presentation.screens.splash

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HabitApplication
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

/**
 * Startup state exposed by [SplashViewModel].
 *
 * @param splashVisible      Whether the splash composable should remain visible.
 * @param isOnboardingComplete Whether the user has completed onboarding (null = not yet loaded).
 * @param destinationRoute   The route to navigate to once splash completes.
 */
data class SplashUiState(
    val splashVisible: Boolean = true,
    val isOnboardingComplete: Boolean? = null,
    val destinationRoute: String? = null,
    val progress: Float = 0f
)

/**
 * ViewModel for the splash screen startup flow.
 *
 * Guarantees:
 * - Splash visible for at least [MIN_SPLASH_DURATION_MS] ms.
 * - Navigation only after BOTH app initialization AND minimum duration are satisfied.
 * - All startup work runs on background dispatchers — main thread is never blocked.
 */
class SplashViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SplashViewModel"
        private const val MIN_SPLASH_DURATION_MS = 2_000L
    }

    private val app = application as HabitApplication

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    /** Timestamp (System.nanoTime) when the splash composable first launched */
    private var splashStartNanos: Long = 0L

    /** Measured durations for diagnostics */
    private var initDurationMs: Long = -1L
    private var totalWaitMs: Long = -1L

    /**
     * Called by [SplashScreen] when the composable enters composition.
     * Starts initialization and timing in parallel, then determines when to navigate.
     */
    fun onSplashStarted() {
        splashStartNanos = System.nanoTime()

        viewModelScope.launch {
            // ── Phase 1: Initialize app services (DB / Repo / UseCases) ──
            _uiState.value = _uiState.value.copy(progress = 0.1f)
            val initTime = measureTimeMillis {
                try {
                    app.ensureInitialized()
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "App initialization timed out", e)
                } catch (e: Exception) {
                    Log.e(TAG, "App initialization failed", e)
                }
            }
            initDurationMs = initTime
            _uiState.value = _uiState.value.copy(progress = 0.5f)
            Log.d(TAG, "App initialization took ${initTime}ms")

            // ── Phase 2: Read DataStore for onboarding flag ──────────
            val isComplete: Boolean
            val dataStoreTime = measureTimeMillis {
                isComplete = app.preferencesManager.isOnboardingCompleteFlow.first()
            }
            Log.d(TAG, "DataStore read took ${dataStoreTime}ms")
            _uiState.value = _uiState.value.copy(
                isOnboardingComplete = isComplete,
                progress = 0.7f
            )

            // ── Phase 3: Enforce minimum splash duration ─────────────
            val elapsedNanos = System.nanoTime() - splashStartNanos
            val elapsedMs = elapsedNanos / 1_000_000L
            val remainingMs = MIN_SPLASH_DURATION_MS - elapsedMs

            if (remainingMs > 0L) {
                Log.d(TAG, "Splash visible for ${elapsedMs}ms, waiting additional ${remainingMs}ms")
                // Advance progress smoothly during wait
                val steps = 20 // more steps for smoother advance
                val delayPerStep = remainingMs / steps
                for (i in 1..steps) {
                    kotlinx.coroutines.delay(delayPerStep)
                    val currentProgress = 0.7f + (0.3f * (i.toFloat() / steps))
                    _uiState.value = _uiState.value.copy(progress = currentProgress)
                }
            } else {
                _uiState.value = _uiState.value.copy(progress = 1.0f)
            }

            totalWaitMs = maxOf(elapsedMs, MIN_SPLASH_DURATION_MS)
            Log.d(TAG, "Total splash visible duration: ${totalWaitMs}ms")
            Log.d(TAG, "--- Cold start timing ---")
            Log.d(TAG, "  Init time:           ${initDurationMs}ms")
            Log.d(TAG, "  Min splash duration: ${MIN_SPLASH_DURATION_MS}ms")
            Log.d(TAG, "  Additional wait:     ${maxOf(0L, MIN_SPLASH_DURATION_MS - initDurationMs - dataStoreTime)}ms")
            Log.d(TAG, "  Total cold start:    ${totalWaitMs}ms")

            // ── Phase 4: Navigate ────────────────────────────────────
            val destination = if (isComplete) {
                com.example.presentation.navigation.Routes.HOME
            } else {
                com.example.presentation.navigation.Routes.ONBOARDING
            }
            _uiState.value = _uiState.value.copy(
                splashVisible = false,
                destinationRoute = destination
            )
        }
    }
}
