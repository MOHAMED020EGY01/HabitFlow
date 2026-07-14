package com.example.presentation.screens.detail

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HabitApplication
import com.example.data.worker.HabitReminderWorker
import com.example.domain.usecase.HabitDetails
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@Immutable
data class DetailUiState(
    val habitDetails: HabitDetails? = null,
    val last7DaysStatus: List<DayStatus> = emptyList(),
    val isLoading: Boolean = true,
    val daysRemaining: Int = 0,
    val previousCycles: List<com.example.domain.model.HabitCycleHistory> = emptyList(),
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
                val appLocale = com.example.util.LocaleDirectionHelper.getLocale(app.currentLanguageCode)
                val langCode = app.currentLanguageCode
                val localizedCtx = com.example.util.LocaleDirectionHelper.getLocalizedContext(app, langCode)
                
                val dayStatuses = last7Days.map { date ->
                    val dateStr = date.toString()
                    val isComp = details.logs.any { it.logDate == dateStr && it.completed }
                    val label = when (date) {
                        LocalDate.now() -> localizedCtx.getString(com.example.R.string.today)
                        LocalDate.now().minusDays(1) -> localizedCtx.getString(com.example.R.string.yesterday)
                        else -> com.example.util.AppFormatters.formatDate(date, "EEEE, d MMM", langCode = langCode)
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
            android.util.Log.d("HabitWidgetSync", "[LOG] HabitDetailViewModel: DB write completed (toggleLog). Timestamp: ${System.currentTimeMillis()}")
            com.example.widget.HabitWidgetSyncUpdater.updateNowForced(app.applicationContext)
        }
    }

    fun pauseHabit() {
        val details = _uiState.value.habitDetails ?: return
        val updatedHabit = details.habit.copy(
            status = com.example.domain.model.HabitStatus.INACTIVE,
            isActive = false,
            inactiveSinceTimestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            app.repository.updateHabit(updatedHabit)
            HabitReminderWorker.cancelHabitReminders(app, updatedHabit.id)
            com.example.widget.HabitWidgetSyncUpdater.updateNowForced(app.applicationContext)
        }
    }

    fun resumeHabit() {
        val details = _uiState.value.habitDetails ?: return
        viewModelScope.launch {
            try {
                com.example.domain.usecase.HabitStatusManager.performDailyRollover(app, app.repository)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val refreshedHabit = app.repository.getHabitById(currentHabitId) ?: details.habit
            val updatedHabit = refreshedHabit.copy(
                status = com.example.domain.model.HabitStatus.ACTIVE,
                isActive = true,
                inactiveSinceTimestamp = null
            )
            app.repository.updateHabit(updatedHabit)
            HabitReminderWorker.scheduleHabitReminders(app, updatedHabit)
            com.example.widget.HabitWidgetSyncUpdater.updateNowForced(app.applicationContext)
        }
    }

    fun restartHabit() {
        val details = _uiState.value.habitDetails ?: return
        viewModelScope.launch {
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
            
            val history = com.example.domain.model.HabitCycleHistory(
                habitId = details.habit.id,
                cycleStartDate = details.habit.cycleStartDate,
                cycleEndDate = details.habit.cycleEndDate,
                completionPercentage = completionPercentage,
                result = resultStatus,
                logsSnapshot = logsJson
            )
            app.repository.insertCycleHistory(history)

            val todayMillis = System.currentTimeMillis()
            val newEndDate = todayMillis + (details.habit.durationDays * 24L * 60L * 60L * 1000L)
            
            val updatedHabit = details.habit.copy(
                status = com.example.domain.model.HabitStatus.ACTIVE,
                isActive = true,
                cycleStartDate = todayMillis,
                cycleEndDate = newEndDate,
                inactiveDaysCount = 0,
                inactiveSinceTimestamp = null
            )

            app.repository.deleteLogsForHabit(details.habit.id)
            app.repository.updateHabit(updatedHabit)
            HabitReminderWorker.scheduleHabitReminders(app, updatedHabit)
            com.example.widget.HabitWidgetSyncUpdater.updateNowForced(app.applicationContext)
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
