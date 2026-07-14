package com.example.presentation.screens.all

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HabitApplication
import com.example.domain.model.Habit
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
    val searchQuery: String = ""
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

    private val _uiState = MutableStateFlow(AllHabitsUiState())
    val uiState: StateFlow<AllHabitsUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        val todayStr = LocalDate.now().toString()
        combine(
            app.repository.getAllHabitsWithCompletion(todayStr),
            _filter,
            _sortBy,
            _searchQuery
        ) { habitsWithCompletion, filter, sortBy, query ->
            var filteredHabits = when (filter) {
                HabitFilter.ALL -> habitsWithCompletion
                HabitFilter.ACTIVE -> habitsWithCompletion.filter { it.habit.status == com.example.domain.model.HabitStatus.ACTIVE }
                HabitFilter.INACTIVE -> habitsWithCompletion.filter { it.habit.status == com.example.domain.model.HabitStatus.INACTIVE }
                HabitFilter.COMPLETE -> habitsWithCompletion.filter { it.habit.status == com.example.domain.model.HabitStatus.COMPLETE }
                HabitFilter.FAILURE -> habitsWithCompletion.filter { it.habit.status == com.example.domain.model.HabitStatus.FAILURE }
            }

            if (query.isNotEmpty()) {
                filteredHabits = filteredHabits.filter {
                    it.habit.name.contains(query, ignoreCase = true) || it.habit.description.contains(query, ignoreCase = true)
                }
            }

            val progressItems = filteredHabits.map { item ->
                HabitProgressItem(item.habit, item.completedCount)
            }

            val sortedItems = when (sortBy) {
                HabitSort.START_DATE -> progressItems.sortedByDescending { it.habit.startedAt ?: it.habit.createdAt }
                HabitSort.PROGRESS -> progressItems.sortedByDescending {
                    val scheduledTotal = it.habit.getScheduledDaysCount()
                    if (scheduledTotal > 0) it.completedDays.toFloat() / scheduledTotal else 0f
                }
            }

            // P9: compute checkedMap only for the filtered subset
            val checkedMap = filteredHabits.associate { item ->
                item.habit.id to item.isCompletedToday
            }

            AllHabitsUiState(
                habitsWithProgress = sortedItems,
                isCheckedTodayMap = checkedMap,
                filter = filter,
                sortBy = sortBy,
                searchQuery = query
            )
        }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun setFilter(filter: HabitFilter) {
        _filter.value = filter
    }

    fun setSortBy(sort: HabitSort) {
        _sortBy.value = sort
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleCheckIn(habitId: Int, completed: Boolean) {
        viewModelScope.launch {
            val todayStr = LocalDate.now().toString()
            android.util.Log.d("HabitWidgetSync", "[LOG] AllHabitsViewModel: Preparing to toggle log for habit $habitId. Timestamp: ${System.currentTimeMillis()}")
            app.repository.toggleLogForDate(habitId, todayStr, completed)
            android.util.Log.d("HabitWidgetSync", "[LOG] AllHabitsViewModel: DB write completed (toggleLog). Timestamp: ${System.currentTimeMillis()}")
            com.example.widget.HabitWidgetSyncUpdater.updateNowForced(app.applicationContext)
        }
    }

    private val _uiEvent = MutableSharedFlow<AllHabitsUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun activateHabit(habitId: Int) {
        viewModelScope.launch {
            val result = app.toggleHabitActiveUseCase(habitId, makeActive = true)
            when (result) {
                is com.example.domain.model.ActivationResult.Activated -> {
                    val habit = app.repository.getHabitById(habitId)
                    if (habit != null) {
                        com.example.data.worker.HabitReminderWorker.scheduleHabitReminders(app, habit)
                    }
                    android.util.Log.d("HabitWidgetSync", "[LOG] AllHabitsViewModel: Habit activated. Timestamp: ${System.currentTimeMillis()}")
                    com.example.widget.HabitWidgetSyncUpdater.updateNowForced(app.applicationContext)
                    _uiEvent.emit(AllHabitsUiEvent.ShowSnackbar(app.applicationContext.getString(com.example.R.string.habit_activated_success)))
                }
                is com.example.domain.model.ActivationResult.SavedAsInactive -> {
                    _uiEvent.emit(AllHabitsUiEvent.ShowSnackbar(
                        app.applicationContext.getString(com.example.R.string.add_habit_limit_reached_desc)
                    ))
                }
                else -> Unit
            }
        }
    }
}

sealed class AllHabitsUiEvent {
    data class ShowSnackbar(val message: String) : AllHabitsUiEvent()
}
