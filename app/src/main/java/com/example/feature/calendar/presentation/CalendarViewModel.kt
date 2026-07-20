package com.example.feature.calendar.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.HabitApplication
import com.example.core.model.domain.Habit
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class CalendarMode { DAY, WEEK }

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val mode: CalendarMode = CalendarMode.DAY,
    val habits: List<Habit> = emptyList(),
    val isLoading: Boolean = true
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HabitApplication

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        observeHabits()
    }

    private fun observeHabits() {
        viewModelScope.launch {
            app.repository.getAllHabits().collect { habits ->
                _uiState.update { it.copy(habits = habits, isLoading = false) }
            }
        }
    }

    fun onDateChange(newDate: LocalDate) {
        _uiState.update { it.copy(selectedDate = newDate) }
    }

    fun toggleMode() {
        _uiState.update { 
            val newMode = if (it.mode == CalendarMode.DAY) CalendarMode.WEEK else CalendarMode.DAY
            it.copy(mode = newMode)
        }
    }

    fun navigatePrevious() {
        val current = _uiState.value
        val newDate = if (current.mode == CalendarMode.DAY) {
            current.selectedDate.minusDays(1)
        } else {
            current.selectedDate.minusWeeks(1)
        }
        _uiState.update { it.copy(selectedDate = newDate) }
    }

    fun navigateNext() {
        val current = _uiState.value
        val newDate = if (current.mode == CalendarMode.DAY) {
            current.selectedDate.plusDays(1)
        } else {
            current.selectedDate.plusWeeks(1)
        }
        _uiState.update { it.copy(selectedDate = newDate) }
    }
}
