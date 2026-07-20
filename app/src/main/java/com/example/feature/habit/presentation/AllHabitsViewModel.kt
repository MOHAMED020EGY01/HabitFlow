package com.example.feature.habit.presentation

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.HabitApplication
import com.example.core.model.domain.Habit
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class HabitFilter { ALL, ACTIVE, INACTIVE, COMPLETE, FAILURE }
enum class HabitSort { START_DATE, PROGRESS }

@Immutable
data class AllHabitsUiState(
    val habitsWithProgress: List<HabitProgressItem> = emptyList(),
    val isCheckedTodayMap: Map<Int, Boolean> = emptyMap(),
    val filter: HabitFilter = HabitFilter.ALL,
    val sortBy: HabitSort = HabitSort.START_DATE,
    val searchQuery: String = "",
    val hasMore: Boolean = false
)

@Immutable
data class HabitProgressItem(
    val habit: Habit,
    val completedDays: Int
)

class AllHabitsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HabitApplication

    private val _filter = MutableStateFlow(HabitFilter.ALL)
    private val _sortBy = MutableStateFlow(HabitSort.START_DATE)
    private val _searchQuery = MutableStateFlow("")
    private val _visibleLimit = MutableStateFlow(10) // يبدأ بـ 10 كما طلبت

    private val _uiState = MutableStateFlow(AllHabitsUiState())
    val uiState: StateFlow<AllHabitsUiState> = _uiState.asStateFlow()

    init {
        observeData()
        enforceActiveLimitIfNeeded()
    }

    private fun enforceActiveLimitIfNeeded() {
        viewModelScope.launch {
            val habits = app.repository.getAllHabitsSync()
            val activeHabits = habits.filter { it.status == com.example.core.model.domain.HabitStatus.ACTIVE }
            if (activeHabits.size > com.example.core.model.domain.MAX_ACTIVE_HABITS) {
                // Keep only the first 6 (ordered by startedAt/createdAt)
                val toDeactivate = activeHabits
                    .sortedBy { it.startedAt ?: it.createdAt }
                    .drop(com.example.core.model.domain.MAX_ACTIVE_HABITS)
                
                toDeactivate.forEach { habit ->
                    app.toggleHabitActiveUseCase(habit.id, makeActive = false)
                }
                
                _uiEvent.emit(AllHabitsUiEvent.ShowSnackbar(
                    app.applicationContext.getString(com.example.R.string.add_habit_limit_warning, com.example.core.model.domain.MAX_ACTIVE_HABITS)
                ))
            }
        }
    }

    private fun observeData() {
        val todayStr = LocalDate.now().toString()
        combine(
            app.repository.getAllHabitsWithCompletion(todayStr),
            _filter,
            _sortBy,
            _searchQuery,
            _visibleLimit
        ) { habitsWithCompletion, filter, sortBy, query, limit ->
            // ... (منطق الفلترة) ...
            var filteredHabits = when (filter) {
                HabitFilter.ALL -> habitsWithCompletion
                HabitFilter.ACTIVE -> habitsWithCompletion.filter { it.habit.status == com.example.core.model.domain.HabitStatus.ACTIVE }
                HabitFilter.INACTIVE -> habitsWithCompletion.filter { it.habit.status == com.example.core.model.domain.HabitStatus.INACTIVE }
                HabitFilter.COMPLETE -> habitsWithCompletion.filter { it.habit.status == com.example.core.model.domain.HabitStatus.COMPLETE }
                HabitFilter.FAILURE -> habitsWithCompletion.filter { it.habit.status == com.example.core.model.domain.HabitStatus.FAILURE }
            }

            if (query.isNotEmpty()) {
                filteredHabits = filteredHabits.filter {
                    it.habit.name.contains(query, ignoreCase = true) || it.habit.description.contains(query, ignoreCase = true)
                }
            }

            val totalCount = filteredHabits.size
            val pagedHabits = filteredHabits.take(limit) // نأخذ فقط العدد المطلوب حالياً

            // P9: Optimization for 902 habits - mapping only on visible subset
            val progressItems = pagedHabits.map { item ->
                HabitProgressItem(item.habit, item.completedCount)
            }

            val sortedItems = when (sortBy) {
                HabitSort.START_DATE -> progressItems.sortedByDescending { it.habit.startedAt ?: it.habit.createdAt }
                HabitSort.PROGRESS -> progressItems.sortedByDescending {
                    val scheduledTotal = it.habit.getScheduledDaysCount()
                    if (scheduledTotal > 0) it.completedDays.toFloat() / scheduledTotal else 0f
                }
            }

            val checkedMap = pagedHabits.associate { item ->
                item.habit.id to item.isCompletedToday
            }

            AllHabitsUiState(
                habitsWithProgress = sortedItems,
                isCheckedTodayMap = checkedMap,
                filter = filter,
                sortBy = sortBy,
                searchQuery = query,
                hasMore = limit < totalCount
            )
        }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun loadMore() {
        if (_uiState.value.hasMore) {
            _visibleLimit.value += 4 // زيادة 4 عادات في كل مرة
        }
    }

    fun setFilter(filter: HabitFilter) {
        _visibleLimit.value = 10 // Reset limit on filter change
        _filter.value = filter
    }

    fun setSortBy(sort: HabitSort) {
        _sortBy.value = sort
    }

    fun setSearchQuery(query: String) {
        _visibleLimit.value = 10 // Reset limit on search
        _searchQuery.value = query
    }

    fun toggleCheckIn(habitId: Int, completed: Boolean) {
        viewModelScope.launch {
            val todayStr = LocalDate.now().toString()
            android.util.Log.d("HabitWidgetSync", "[LOG] AllHabitsViewModel: Preparing to toggle log for habit $habitId. Timestamp: ${System.currentTimeMillis()}")
            app.repository.toggleLogForDate(habitId, todayStr, completed)
            android.util.Log.d("HabitWidgetSync", "[LOG] AllHabitsViewModel: DB write completed (toggleLog). Timestamp: ${System.currentTimeMillis()}")
            com.example.core.infrastructure.widget.HabitWidgetSyncUpdater.updateNowForced(app.applicationContext)
        }
    }

    private val _uiEvent = MutableSharedFlow<AllHabitsUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun activateHabit(habitId: Int) {
        viewModelScope.launch {
            val result = app.toggleHabitActiveUseCase(habitId, makeActive = true)
            when (result) {
                is com.example.core.model.domain.ActivationResult.Activated -> {
                    val habit = app.repository.getHabitById(habitId)
                    if (habit != null) {
                        com.example.core.infrastructure.worker.HabitReminderWorker.scheduleHabitReminders(app, habit)
                    }
                    android.util.Log.d("HabitWidgetSync", "[LOG] AllHabitsViewModel: Habit activated. Timestamp: ${System.currentTimeMillis()}")
                    com.example.core.infrastructure.widget.HabitWidgetSyncUpdater.updateNowForced(app.applicationContext)
                    _uiEvent.emit(AllHabitsUiEvent.ShowSnackbar(app.applicationContext.getString(com.example.R.string.habit_activated_success)))
                }
                is com.example.core.model.domain.ActivationResult.SavedAsInactive -> {
                    val habit = app.repository.getHabitById(habitId)
                    if (habit != null) {
                        _uiEvent.emit(AllHabitsUiEvent.ShowSwapDialog(habit))
                    }
                }
                else -> Unit
            }
        }
    }

    fun swapHabits(toActivateId: Int, toDeactivateId: Int) {
        viewModelScope.launch {
            app.swapHabitsUseCase(toActivateId, toDeactivateId)
            _uiEvent.emit(AllHabitsUiEvent.ShowSnackbar(app.applicationContext.getString(com.example.R.string.habit_activated_success)))
        }
    }
}

sealed class AllHabitsUiEvent {
    data class ShowSnackbar(val message: String) : AllHabitsUiEvent()
    data class ShowSwapDialog(val habitToActivate: Habit) : AllHabitsUiEvent()
}
