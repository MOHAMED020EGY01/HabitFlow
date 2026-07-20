package com.example.core.model.domain

import androidx.compose.runtime.Immutable

@Immutable
data class HabitWithCompletion(
    val habit: Habit,
    val completedCount: Int,
    val isCompletedToday: Boolean
)
