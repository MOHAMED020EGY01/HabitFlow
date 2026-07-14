package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Habit
import com.example.domain.model.HabitStatus

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
    val inactiveSinceTimestamp: Long? = null
) {
    fun toDomain(): Habit = Habit(
        id = id,
        name = name,
        description = description,
        durationDays = durationDays,
        colorHex = colorHex,
        isActive = isActive,
        reminderTimes = reminderTimes,
        createdAt = createdAt,
        startedAt = startedAt,
        status = try { HabitStatus.valueOf(status) } catch(e: Exception) { HabitStatus.ACTIVE },
        cycleStartDate = if (cycleStartDate == 0L) (startedAt ?: createdAt) else cycleStartDate,
        cycleEndDate = if (cycleEndDate == 0L) ((startedAt ?: createdAt) + durationDays * 24L * 60L * 60L * 1000L) else cycleEndDate,
        inactiveDaysCount = inactiveDaysCount,
        activeDays = activeDays.mapNotNull { name ->
            try { java.time.DayOfWeek.valueOf(name) } catch (_: Exception) { null }
        }.toSet(),
        inactiveSinceTimestamp = inactiveSinceTimestamp
    )

    companion object {
        fun fromDomain(habit: Habit): HabitEntity = HabitEntity(
            id = habit.id,
            name = habit.name,
            description = habit.description,
            durationDays = habit.durationDays,
            colorHex = habit.colorHex,
            isActive = habit.isActive,
            reminderTimes = habit.reminderTimes,
            createdAt = habit.createdAt,
            startedAt = habit.startedAt,
            status = habit.status.name,
            cycleStartDate = habit.cycleStartDate,
            cycleEndDate = habit.cycleEndDate,
            inactiveDaysCount = habit.inactiveDaysCount,
            activeDays = habit.activeDays.map { it.name },
            inactiveSinceTimestamp = habit.inactiveSinceTimestamp
        )
    }
}

