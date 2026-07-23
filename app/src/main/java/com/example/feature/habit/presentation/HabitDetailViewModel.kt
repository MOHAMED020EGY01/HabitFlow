package com.example.feature.habit.presentation

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.HabitApplication
import com.example.core.infrastructure.worker.HabitReminderWorker
import com.example.feature.habit.domain.HabitDetails
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@Immutable
data class DetailUiState(
    val habitDetails: HabitDetails? = null,
    val last7DaysStatus: List<DayStatus> = emptyList(),
    val isLoading: Boolean = true,
    val daysRemaining: Int = 0,
    val previousCycles: List<com.example.core.model.domain.HabitCycleHistory> = emptyList(),
    val missedDaysCount: Int = 0
)

@Immutable
data class DayStatus(
    val date: LocalDate,
    val dateString: String, // "yyyy-MM-dd"
    val displayLabel: String, // "Today", "Yesterday", "Monday 29"
    val isCompleted: Boolean
)

class HabitDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HabitApplication

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var currentHabitId: Int = 0

    fun observeHabitDetails(habitId: Int) {
        if (currentHabitId == habitId && _uiState.value.habitDetails != null) {
            return
        }
        currentHabitId = habitId
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            combine(
                app.getHabitDetailsUseCase(habitId),
                app.repository.getCycleHistoryForHabit(habitId)
            ) { details, history ->
                if (details == null) return@combine null
                
                val last7Days = (0..6).map { LocalDate.now().minusDays(it.toLong()) }
                val appLocale = com.example.core.util.LocaleDirectionHelper.getLocale(app.currentLanguageCode)
                val langCode = app.currentLanguageCode
                val localizedCtx = com.example.core.util.LocaleDirectionHelper.getLocalizedContext(app, langCode)
                
                val dayStatuses = last7Days.map { date ->
                    val dateStr = date.toString()
                    val isComp = details.logs.any { it.logDate == dateStr && it.completed }
                    val label = when (date) {
                        LocalDate.now() -> localizedCtx.getString(com.example.R.string.today)
                        LocalDate.now().minusDays(1) -> localizedCtx.getString(com.example.R.string.yesterday)
                        else -> com.example.core.util.AppFormatters.formatDate(date, "EEEE, d MMM", langCode = langCode)
                    }
                    DayStatus(date, dateStr, label, isComp)
                }

                val habit = details.habit
                val daysRemaining = (habit.getScheduledDaysCount() - details.completedDays).coerceAtLeast(0)

                Triple(details, dayStatuses, Pair(daysRemaining, history))
            }
            .flowOn(kotlinx.coroutines.Dispatchers.Default)
            .collectLatest { result ->
                if (result != null) {
                    val (details, dayStatuses, pair) = result
                    val (daysRemaining, history) = pair
                    val missedDaysCount = details.logs.count { it.state == "MISS" }
                    _uiState.update {
                        it.copy(
                            habitDetails = details,
                            last7DaysStatus = dayStatuses,
                            isLoading = false,
                            daysRemaining = daysRemaining,
                            previousCycles = history,
                            missedDaysCount = missedDaysCount
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun toggleLogForDate(dateStr: String, completed: Boolean) {
        viewModelScope.launch {
            android.util.Log.d("HabitWidgetSync", "[LOG] HabitDetailViewModel: Toggling log for date $dateStr. Timestamp: ${System.currentTimeMillis()}")
            app.repository.toggleLogForDate(currentHabitId, dateStr, completed)
            
            // Check for instant cycle completion if this was the last day
            if (completed && dateStr == LocalDate.now().toString()) {
                com.example.core.domain.usecase.HabitStatusManager.checkHabitCompletion(
                    app.applicationContext, 
                    app.repository, 
                    currentHabitId
                )
            }

            android.util.Log.d("HabitWidgetSync", "[LOG] HabitDetailViewModel: DB write completed (toggleLog). Timestamp: ${System.currentTimeMillis()}")
            com.example.core.infrastructure.widget.HabitWidgetSyncUpdater.updateNowForced(app.applicationContext)
        }
    }

    fun pauseHabit() {
        viewModelScope.launch {
            app.toggleHabitActiveUseCase(currentHabitId, makeActive = false)
            // No need to manually update state here, the observer will catch it
        }
    }

    fun resumeHabit() {
        viewModelScope.launch {
            try {
                com.example.core.domain.usecase.HabitStatusManager.performDailyRollover(app, app.repository)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            val result = app.toggleHabitActiveUseCase(currentHabitId, makeActive = true)
            if (result is com.example.core.model.domain.ActivationResult.SavedAsInactive) {
                // We can't use a shared flow for snackbars here easily without adding it to the UI state
                // but we can at least ensure the limit is enforced.
                // For now, let's just make sure it doesn't bypass the limit.
            }
        }
    }

    fun restartHabit() {
        val details = _uiState.value.habitDetails ?: return
        viewModelScope.launch {
            val activeCount = app.repository.getActiveHabitsCount()
            val willBeActive = activeCount < com.example.core.model.domain.MAX_ACTIVE_HABITS

            val logs = app.repository.getLogsForHabitSync(details.habit.id)
            val doneCount = logs.count { it.state == "DONE" || it.completed }
            val missCount = logs.count { it.state == "MISS" }
            val totalCount = doneCount + missCount
            val completionPercentage = if (totalCount > 0) {
                (doneCount.toDouble() / totalCount.toDouble()) * 100.0
            } else {
                0.0
            }
            val resultStatus = if (completionPercentage >= 90.0) "COMPLETE" else "FAILURE"

            val gson = com.google.gson.Gson()
            val logsJson = gson.toJson(logs.map { mapOf("date" to it.logDate, "state" to it.state) })
            
            val history = com.example.core.model.domain.HabitCycleHistory(
                habitId = details.habit.id,
                cycleStartDate = details.habit.cycleStartDate,
                cycleEndDate = details.habit.cycleEndDate ?: details.habit.cycleStartDate,
                completionPercentage = completionPercentage,
                result = resultStatus,
                logsSnapshot = logsJson
            )
            app.repository.insertCycleHistory(history)

            val todayMillis = System.currentTimeMillis()
            val newEndDate = if (details.habit.durationType == com.example.core.model.domain.HabitDurationType.CALENDAR) {
                todayMillis + (details.habit.durationDays * 24L * 60L * 60L * 1000L)
            } else null
            
            val updatedHabit = details.habit.copy(
                status = if (willBeActive) com.example.core.model.domain.HabitStatus.ACTIVE else com.example.core.model.domain.HabitStatus.INACTIVE,
                isActive = willBeActive,
                cycleStartDate = todayMillis,
                cycleEndDate = newEndDate,
                inactiveDaysCount = 0,
                inactiveSinceTimestamp = if (willBeActive) null else todayMillis
            )

            app.repository.deleteLogsForHabit(details.habit.id)
            app.repository.updateHabit(updatedHabit)
            if (willBeActive) {
                HabitReminderWorker.scheduleHabitReminders(app, updatedHabit)
            } else {
                HabitReminderWorker.cancelHabitReminders(app, updatedHabit.id)
            }
            com.example.core.infrastructure.widget.HabitWidgetSyncUpdater.updateNowForced(app.applicationContext)
        }
    }

    fun deleteHabit(onComplete: () -> Unit) {
        val details = _uiState.value.habitDetails ?: return
        viewModelScope.launch {
            android.util.Log.d("HabitWidgetSync", "[LOG] HabitDetailViewModel: Deleting habit ${details.habit.id}. Timestamp: ${System.currentTimeMillis()}")
            app.deleteHabitUseCase(details.habit)
            HabitReminderWorker.cancelHabitReminders(app, details.habit.id)
            android.util.Log.d("HabitWidgetSync", "[LOG] HabitDetailViewModel: UseCase completed (delete, sync handled by UseCase). Timestamp: ${System.currentTimeMillis()}")
            onComplete()
        }
    }
}
