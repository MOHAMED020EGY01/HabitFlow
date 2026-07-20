package com.example.core.model.domain

data class HabitCycleHistory(
    val id: Int = 0,
    val habitId: Int,
    val cycleStartDate: Long,
    val cycleEndDate: Long,
    val completionPercentage: Double,
    val result: String, // "COMPLETE" or "FAILURE"
    val logsSnapshot: String // JSON representation or plain description
)
