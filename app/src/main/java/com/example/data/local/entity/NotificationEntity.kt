package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.HabitNotification

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String,
    val timestamp: Long,
    val type: String
)

fun NotificationEntity.toDomain() = HabitNotification(
    id = id,
    title = title,
    body = body,
    timestamp = timestamp,
    type = type
)

fun HabitNotification.toEntity() = NotificationEntity(
    id = id,
    title = title,
    body = body,
    timestamp = timestamp,
    type = type
)
