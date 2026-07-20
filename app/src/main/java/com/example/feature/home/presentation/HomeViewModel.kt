package com.example.feature.home.presentation

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.HabitApplication
import com.example.core.model.domain.Habit
import com.example.core.util.StreakCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@Immutable
data class HabitWithProgress(
    val habit: Habit,
    val completedDays: Int,
    val streakDays: Int = 0
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HabitApplication

    // === Independent reactive state slices ===
    // Each slice can update independently, preventing full-list recomposition.

    /** Loading state — true until first data arrives */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** User profile (DataStore-only) — changes independently of habit data */
    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userPhotoUri = MutableStateFlow("")
    val userPhotoUri: StateFlow<String> = _userPhotoUri.asStateFlow()

    private val _isReliableBannerDismissed = MutableStateFlow(false)
    val isReliableBannerDismissed: StateFlow<Boolean> = _isReliableBannerDismissed.asStateFlow()

    /** Static quote — set once at init */
    private val _motivationalQuote = MutableStateFlow("")
    val motivationalQuote: StateFlow<String> = _motivationalQuote.asStateFlow()

    /** Habit list using SnapshotStateList — item-level mutations mean
     *  only the affected LazyColumn item recomposes on change */
    private val _habits = mutableStateListOf<HabitWithProgress>()
    val habits: List<HabitWithProgress> get() = _habits

    /** Checked map using SnapshotStateMap — individual key updates */
    private val _checkedMap = mutableStateMapOf<Int, Boolean>()
    val checkedMap: Map<Int, Boolean> get() = _checkedMap

    /** UI Events Flow */
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        // Observe language changes to update the motivational quote dynamically
        app.preferencesManager.appLanguageFlow
            .onEach { _ ->
                val quoteProvider = com.example.core.util.MotivationalQuoteProvider(app)
                _motivationalQuote.value = quoteProvider.getRandomQuote()
            }
            .launchIn(viewModelScope)

        // Defer data observation to after UI is mounted
        viewModelScope.launch { observeData() }
    }

    private suspend fun observeData() {
        // No need to call ensureInitialized() here — the SplashScreen already
        // did it before navigating. This is a safety net in case the ViewModel
        // is created via a different path.
        val todayStr = LocalDate.now().toString()

        // Pre-compute first emission futures so we can batch the initial
        // loading state into a single "ready" transition instead of
        // shimmer → empty → data.
        val userDataDeferred = viewModelScope.async {
            combine(
                app.preferencesManager.userNameFlow,
                app.preferencesManager.userPhotoUriFlow,
                app.preferencesManager.isReliableBannerDismissedFlow
            ) { name, photo, bannerDismissed ->
                Triple(name, photo, bannerDismissed)
            }.first()
        }

        val habitsDeferred = viewModelScope.async {
            app.repository
                .getActiveHabitsWithCompletion(todayStr)
                .distinctUntilChanged()
                .flowOn(Dispatchers.Default)
                .first()
        }

        // Await FIRST emission from BOTH sources before setting isLoading=false
        val (name, photo, bannerDismissed) = userDataDeferred.await()
        val initialHabits = habitsDeferred.await()

        _userName.value = name
        _userPhotoUri.value = photo
        _isReliableBannerDismissed.value = bannerDismissed
        initialHabits.forEach { item ->
            _habits.add(HabitWithProgress(
                habit = item.habit,
                completedDays = item.completedCount,
                streakDays = if (item.isCompletedToday) item.completedCount.coerceAtMost(1) else 0
            ))
            _checkedMap[item.habit.id] = item.isCompletedToday
        }
        _isLoading.value = false

        // Now continue observing for LIVE updates (after initial load)
        viewModelScope.launch {
            combine(
                app.preferencesManager.userNameFlow,
                app.preferencesManager.userPhotoUriFlow,
                app.preferencesManager.isReliableBannerDismissedFlow
            ) { n, p, b -> Triple(n, p, b) }.drop(1).collect { (n, p, b) ->
                _userName.value = n
                _userPhotoUri.value = p
                _isReliableBannerDismissed.value = b
            }
        }

        viewModelScope.launch {
            app.repository
                .getActiveHabitsWithCompletion(todayStr)
                .distinctUntilChanged()
                .flowOn(Dispatchers.Default)
                .drop(1) // skip the initial emission (already handled)
                .collect { items ->
                    _habits.clear()
                    _checkedMap.clear()
                    items.forEach { item ->
                        _habits.add(HabitWithProgress(
                            habit = item.habit,
                            completedDays = item.completedCount,
                            streakDays = if (item.isCompletedToday) item.completedCount.coerceAtMost(1) else 0
                        ))
                        _checkedMap[item.habit.id] = item.isCompletedToday
                    }
                }
        }
    }

    fun toggleCheckIn(habitId: Int, completed: Boolean) {
        val habitItem = _habits.find { it.habit.id == habitId }
        if (habitItem != null && !habitItem.habit.isActiveToday() && completed) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowSnackbar(app.getString(com.example.R.string.habit_not_scheduled_today)))
            }
            return
        }

        viewModelScope.launch {
            val todayStr = LocalDate.now().toString()
            app.repository.toggleLogForDate(habitId, todayStr, completed)
            StreakCalculator.invalidateCache(habitId)
            
            // Check for instant cycle completion if this was the last day
            if (completed) {
                com.example.core.domain.usecase.HabitStatusManager.checkHabitCompletion(
                    app.applicationContext, 
                    app.repository, 
                    habitId
                )
            }

            com.example.core.infrastructure.widget.HabitWidgetSyncUpdater.updateNowForced(app.applicationContext)
        }
    }

    fun dismissReliableBanner() {
        viewModelScope.launch {
            app.preferencesManager.saveReliableBannerDismissed(true)
        }
    }
}
