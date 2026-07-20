package com.example.core.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.domain.HabitNotification

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String,
    val timestamp: Long,
    val type: String
)

