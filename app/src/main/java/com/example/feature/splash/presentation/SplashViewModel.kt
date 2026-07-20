package com.example.feature.splash.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.HabitApplication
import com.example.core.navigation.Routes
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

/**
 * Startup state exposed by [SplashViewModel].
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
 */
class SplashViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SplashViewModel"
        private const val MIN_SPLASH_DURATION_MS = 3_000L // 3 Seconds as requested
    }

    private val app = application as HabitApplication

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private var splashStartNanos: Long = 0L

    fun onSplashStarted() {
        splashStartNanos = System.nanoTime()

        viewModelScope.launch {
            // ── Phase 1: Initialize app services (DB / Repo / UseCases) ──
            _uiState.value = _uiState.value.copy(progress = 0.1f)
            try {
                app.ensureInitialized()
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "App initialization timed out", e)
            } catch (e: Exception) {
                Log.e(TAG, "App initialization failed", e)
            }
            _uiState.value = _uiState.value.copy(progress = 0.5f)

            // ── Phase 2: Read DataStore for onboarding flag ──────────
            val isComplete = app.preferencesManager.isOnboardingCompleteFlow.first()
            _uiState.value = _uiState.value.copy(
                isOnboardingComplete = isComplete,
                progress = 0.7f
            )

            // ── Phase 3: Enforce minimum splash duration ─────────────
            val elapsedMs = (System.nanoTime() - splashStartNanos) / 1_000_000L
            val remainingMs = MIN_SPLASH_DURATION_MS - elapsedMs

            if (remainingMs > 0L) {
                // Smoothly advance progress during the remaining time
                val steps = 30 
                val delayPerStep = remainingMs / steps
                for (i in 1..steps) {
                    kotlinx.coroutines.delay(delayPerStep)
                    val currentProgress = 0.7f + (0.3f * (i.toFloat() / steps))
                    _uiState.value = _uiState.value.copy(progress = currentProgress)
                }
            } else {
                _uiState.value = _uiState.value.copy(progress = 1.0f)
            }

            // ── Phase 4: Determine Destination ────────────────────────
            val destination = if (isComplete) {
                // Navigate to Main Pager (Home)
                Routes.MAIN_PAGER.replace("{initialTab}", "0")
            } else {
                Routes.ONBOARDING
            }
            
            _uiState.value = _uiState.value.copy(
                splashVisible = false,
                destinationRoute = destination
            )
        }
    }
}
