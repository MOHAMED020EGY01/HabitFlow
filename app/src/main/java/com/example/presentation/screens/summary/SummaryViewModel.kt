package com.example.presentation.screens.summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HabitApplication
import com.example.domain.usecase.HabitsSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HabitApplication

    private val _summaryState = MutableStateFlow<HabitsSummary?>(null)
    val summaryState: StateFlow<HabitsSummary?> = _summaryState.asStateFlow()

    init {
        app.getHabitsSummaryUseCase()
            .onEach { _summaryState.value = it }
            .launchIn(viewModelScope)
    }
}
