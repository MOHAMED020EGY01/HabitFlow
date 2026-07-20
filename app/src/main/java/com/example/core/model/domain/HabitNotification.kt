package com.example.core.model.domain

data class HabitNotification(
    val id: Int = 0,
    val title: String,
    val body: String,
    val timestamp: Long,
    val type: String
)
