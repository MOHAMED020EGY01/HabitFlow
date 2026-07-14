package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.HabitLog

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["habitId", "logDate"], unique = true)
    ]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val logDate: String, // "yyyy-MM-dd"
    val completed: Boolean,
    val state: String = if (completed) "DONE" else "MISS"
) {
    fun toDomain(): HabitLog = HabitLog(
        id = id,
        habitId = habitId,
        logDate = logDate,
        completed = completed,
        state = state
    )

    companion object {
        fun fromDomain(log: HabitLog): HabitLogEntity = HabitLogEntity(
            id = log.id,
            habitId = log.habitId,
            logDate = log.logDate,
            completed = log.completed,
            state = log.state
        )
    }
}

