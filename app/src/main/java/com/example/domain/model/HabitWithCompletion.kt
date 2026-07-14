package com.example.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class HabitWithCompletion(
    val habit: Habit,
    val completedCount: Int,
    val isCompletedToday: Boolean
)
