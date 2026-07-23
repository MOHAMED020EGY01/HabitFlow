package com.example.core.model.domain

import java.time.DayOfWeek

data class HabitWithWidgetInfo(
    val habitId: Int,
    val name: String,
    val colorHex: String,
    val daysCompleted: Int,
    val totalDays: Int,
    val daysRemaining: Int,
    val progressPercent: Float,
    val isCompletedToday: Boolean,
    val reminderTimes: List<String>,
    val activeDays: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val durationType: HabitDurationType = HabitDurationType.CALENDAR
)
