package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.HabitCycleHistory

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
    fun toDomain(): HabitCycleHistory = HabitCycleHistory(
        id = id,
        habitId = habitId,
        cycleStartDate = cycleStartDate,
        cycleEndDate = cycleEndDate,
        completionPercentage = completionPercentage,
        result = result,
        logsSnapshot = logsSnapshot
    )

    companion object {
        fun fromDomain(history: HabitCycleHistory): HabitCycleHistoryEntity = HabitCycleHistoryEntity(
            id = history.id,
            habitId = history.habitId,
            cycleStartDate = history.cycleStartDate,
            cycleEndDate = history.cycleEndDate,
            completionPercentage = history.completionPercentage,
            result = history.result,
            logsSnapshot = history.logsSnapshot
        )
    }
}
