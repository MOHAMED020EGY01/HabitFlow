package com.example.presentation.screens.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HabitApplication
import com.example.data.worker.HabitReminderWorker
import com.example.domain.model.Habit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import com.example.domain.model.ActivationResult
import androidx.compose.runtime.Immutable
import java.time.DayOfWeek

@Immutable
data class AddHabitUiState(
    val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val durationDays: Int = 30,
    val colorHex: String = "#E53935",
    val isActive: Boolean = true,
    val reminderTimes: List<String> = listOf("09:00"),
    val isEditMode: Boolean = false,
    val wasActiveBefore: Boolean = false,
    val nameError: String? = null,
    val durationError: String? = null,
    val reminderTimesError: String? = null,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val activeDays: Set<DayOfWeek> = DayOfWeek.values().toSet()
)

class AddHabitViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HabitApplication

    val activeHabitsCount: StateFlow<Int> = app.getActiveHabitsCountUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _saveResult = MutableSharedFlow<ActivationResult>()
    val saveResult = _saveResult.asSharedFlow()

    private val _uiState = MutableStateFlow(AddHabitUiState())
    val uiState: StateFlow<AddHabitUiState> = _uiState.asStateFlow()

    private fun getLocalizedContext(): android.content.Context {
        val langCode = runBlocking { app.preferencesManager.appLanguageFlow.first() }
        return com.example.util.LocaleDirectionHelper.getLocalizedContext(app, langCode)
    }

    fun loadHabit(habitId: Int) {
        if (habitId <= 0) return
        viewModelScope.launch {
            val habit = app.repository.getHabitById(habitId)
            if (habit != null) {
                _uiState.update {
                    it.copy(
                        id = habit.id,
                        name = habit.name,
                        description = habit.description,
                        durationDays = habit.durationDays,
                        colorHex = habit.colorHex,
                        isActive = habit.isActive,
                        reminderTimes = habit.reminderTimes,
                        isEditMode = true,
                        wasActiveBefore = habit.isActive,
                        activeDays = habit.activeDays
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun onDescriptionChange(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    fun onDurationChange(duration: Int) {
        _uiState.update { it.copy(durationDays = duration, durationError = null) }
    }

    fun onColorChange(colorHex: String) {
        _uiState.update { it.copy(colorHex = colorHex) }
    }

    fun onActiveChange(isActive: Boolean) {
        _uiState.update { it.copy(isActive = isActive) }
    }

    fun onActiveDaysChange(days: Set<DayOfWeek>) {
        _uiState.update { it.copy(activeDays = days) }
    }

    fun addReminderTime(time: String) {
        viewModelScope.launch {
            val currentTimes = _uiState.value.reminderTimes
            if (currentTimes.contains(time)) {
                _uiState.update { it.copy(errorMessage = getLocalizedContext().getString(com.example.R.string.add_habit_error_time_exists)) }
                return@launch
            }

            val editingHabitId = if (_uiState.value.isEditMode) _uiState.value.id else null
            val proposedActiveDays = _uiState.value.activeDays
            
            when (val result = app.validateReminderTimeUseCase.validate(time, proposedActiveDays, editingHabitId, currentTimes)) {
                is com.example.domain.usecase.ValidateReminderTimeUseCase.ValidationResult.Conflict -> {
                    val isArabic = com.example.util.LocaleDirectionHelper.isRtl(app.currentLanguageCode)
                    val formattedConflictingTime = formatTimeLocalized(result.conflictingTime, isArabic)
                    val habitName = if (result.conflictingHabitName == "this habit") {
                        getLocalizedContext().getString(com.example.R.string.this_habit)
                    } else result.conflictingHabitName

                    _uiState.update { it.copy(errorMessage = getLocalizedContext().getString(com.example.R.string.add_habit_error_time_conflict, habitName, formattedConflictingTime)) }
                }
                com.example.domain.usecase.ValidateReminderTimeUseCase.ValidationResult.Valid -> {
                    _uiState.update {
                        it.copy(
                            reminderTimes = it.reminderTimes + time,
                            reminderTimesError = null,
                            errorMessage = null
                        )
                    }
                }
            }
        }
    }

    private fun formatTimeLocalized(time: String, isArabic: Boolean): String {
        return try {
            val parts = time.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            com.example.util.AppFormatters.formatTime(hour, minute, isArabic)
        } catch (_: Exception) {
            time
        }
    }

    fun removeReminderTime(time: String) {
        _uiState.update { 
            val updatedTimes = it.reminderTimes - time
            it.copy(
                reminderTimes = updatedTimes,
                reminderTimesError = if (updatedTimes.isEmpty()) app.getString(com.example.R.string.add_habit_error_no_reminders) else null,
                errorMessage = null
            ) 
        }
    }

    fun saveHabit() {
        val name = _uiState.value.name.trim()
        val duration = _uiState.value.durationDays
        val reminders = _uiState.value.reminderTimes

        var hasError = false
        val localizedCtx = getLocalizedContext()

        if (name.isEmpty()) {
            _uiState.update { it.copy(nameError = localizedCtx.getString(com.example.R.string.habit_name_empty_error)) }
            hasError = true
        } else {
            _uiState.update { it.copy(nameError = null) }
        }

        if (duration <= 0) {
            _uiState.update { it.copy(durationError = localizedCtx.getString(com.example.R.string.add_habit_error_duration)) }
            hasError = true
        } else {
            _uiState.update { it.copy(durationError = null) }
        }

        if (reminders.isEmpty()) {
            _uiState.update { it.copy(reminderTimesError = localizedCtx.getString(com.example.R.string.add_habit_error_no_reminders)) }
            hasError = true
        } else {
            _uiState.update { it.copy(reminderTimesError = null) }
        }

        if (hasError) {
            return
        }

        viewModelScope.launch {
            // Final validation of all reminder times against other habits before saving
            val editingHabitId = if (_uiState.value.isEditMode) _uiState.value.id else null
            val proposedActiveDays = _uiState.value.activeDays
            val reminderTimes = _uiState.value.reminderTimes

            for (i in reminderTimes.indices) {
                val time = reminderTimes[i]
                val otherTimes = reminderTimes.filterIndexed { index, _ -> index != i }
                val result = app.validateReminderTimeUseCase.validate(time, proposedActiveDays, editingHabitId, otherTimes)
                if (result is com.example.domain.usecase.ValidateReminderTimeUseCase.ValidationResult.Conflict) {
                    val isArabic = com.example.util.LocaleDirectionHelper.isRtl(app.currentLanguageCode)
                    val localizedCtx = getLocalizedContext()
                    val formattedTime = formatTimeLocalized(result.conflictingTime, isArabic)
                    val habitName = if (result.conflictingHabitName == "this habit") {
                        localizedCtx.getString(com.example.R.string.this_habit)
                    } else result.conflictingHabitName

                    val message = localizedCtx.getString(
                        com.example.R.string.add_habit_error_time_conflict_multi,
                        formatTimeLocalized(time, isArabic),
                        habitName,
                        formattedTime
                    )
                    _uiState.update { it.copy(errorMessage = message) }
                    return@launch
                }
            }

            val habit = Habit(
                id = _uiState.value.id,
                name = name,
                description = _uiState.value.description.trim(),
                durationDays = duration,
                colorHex = _uiState.value.colorHex,
                isActive = _uiState.value.isActive,
                reminderTimes = _uiState.value.reminderTimes,
                createdAt = System.currentTimeMillis(),
                startedAt = System.currentTimeMillis(),
                activeDays = _uiState.value.activeDays
            )

            android.util.Log.d("HabitWidgetSync", "[LOG] AddHabitViewModel: [DB WRITE START] Initiating DB write. Timestamp: ${System.currentTimeMillis()}")

            val (result, finalHabit) = if (_uiState.value.isEditMode) {
                val oldHabit = app.repository.getHabitById(habit.id)
                val finalHabitDraft = habit.copy(
                    createdAt = oldHabit?.createdAt ?: habit.createdAt,
                    startedAt = oldHabit?.startedAt ?: habit.startedAt
                )
                app.updateHabitUseCase(finalHabitDraft, _uiState.value.wasActiveBefore)
            } else {
                app.addHabitUseCase(habit)
            }

            android.util.Log.d("HabitWidgetSync", "[LOG] AddHabitViewModel: [DB WRITE END] DB write completed successfully. Timestamp: ${System.currentTimeMillis()}")

            // Immediately emit saveResult to trigger navigation back to Home/previous screen
            _saveResult.emit(result)
            _uiState.update { it.copy(saveSuccess = true) }

            // In a separate coroutine launched on applicationScope, run reminders and widget sync concurrently
            app.applicationScope.launch {
                android.util.Log.d("HabitWidgetSync", "[LOG] AddHabitViewModel: [BG WORK START] Launching async background tasks. Timestamp: ${System.currentTimeMillis()}")
                try {
                    coroutineScope {
                        launch {
                            if (finalHabit.isActive) {
                                android.util.Log.d("HabitWidgetSync", "[LOG] AddHabitViewModel: Scheduling reminders. Timestamp: ${System.currentTimeMillis()}")
                                HabitReminderWorker.scheduleHabitReminders(app.applicationContext, finalHabit)
                            } else {
                                android.util.Log.d("HabitWidgetSync", "[LOG] AddHabitViewModel: Cancelling reminders. Timestamp: ${System.currentTimeMillis()}")
                                HabitReminderWorker.cancelHabitReminders(app.applicationContext, finalHabit.id)
                            }
                        }
                        launch {
                            android.util.Log.d("HabitWidgetSync", "[LOG] AddHabitViewModel: Syncing widgets. Timestamp: ${System.currentTimeMillis()}")
                            com.example.widget.HabitWidgetSyncUpdater.updateNowForced(app.applicationContext)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HabitWidgetSync", "[LOG] AddHabitViewModel: Error in async background tasks", e)
                }
                android.util.Log.d("HabitWidgetSync", "[LOG] AddHabitViewModel: [BG WORK END] Async background tasks completed. Timestamp: ${System.currentTimeMillis()}")
            }
        }
    }
}
