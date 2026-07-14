package com.example.domain.model

data class HabitLog(
    val id: Int = 0,
    val habitId: Int,
    val logDate: String, // "yyyy-MM-dd"
    val completed: Boolean,
    val state: String = if (completed) "DONE" else "MISS"
)

