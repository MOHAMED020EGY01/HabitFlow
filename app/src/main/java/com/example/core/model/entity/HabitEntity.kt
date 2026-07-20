package com.example.core.model.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.domain.Habit
import com.example.core.model.domain.HabitStatus

@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["isActive"]),
        Index(value = ["createdAt"]),
        Index(value = ["startedAt"])
    ]
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val durationDays: Int,
    val colorHex: String,
    val isActive: Boolean,
    val reminderTimes: List<String>, // Converted automatically via Converters
    val createdAt: Long,
    val startedAt: Long?,
    val status: String = "ACTIVE",
    val cycleStartDate: Long = 0,
    val cycleEndDate: Long = 0,
    val inactiveDaysCount: Int = 0,
    val activeDays: List<String> = listOf("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"),
    val inactiveSinceTimestamp: Long? = null,
    val reminderVoice: String = "DEFAULT"
) {
    // Mappers moved to HabitMapper.kt
}

