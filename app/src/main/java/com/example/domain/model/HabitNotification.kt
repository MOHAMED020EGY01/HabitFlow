package com.example.domain.model

data class HabitNotification(
    val id: Int = 0,
    val title: String,
    val body: String,
    val timestamp: Long,
    val type: String
)
