package com.example.core.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.domain.HabitCycleHistory

@Entity(
    tableName = "habit_cycle_history",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId"])
    ]
)
data class HabitCycleHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val cycleStartDate: Long,
    val cycleEndDate: Long,
    val completionPercentage: Double,
    val result: String, // "COMPLETE" or "FAILURE"
    val logsSnapshot: String
) {
    // Mappers moved to HabitMapper.kt
}
