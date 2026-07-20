package com.example.core.model.mapper

import com.example.core.model.domain.Habit
import com.example.core.model.domain.HabitCycleHistory
import com.example.core.model.domain.HabitLog
import com.example.core.model.domain.HabitNotification
import com.example.core.model.domain.HabitStatus
import com.example.core.model.entity.HabitCycleHistoryEntity
import com.example.core.model.entity.HabitEntity
import com.example.core.model.entity.HabitLogEntity
import com.example.core.model.entity.NotificationEntity
import java.time.DayOfWeek

fun HabitEntity.toDomain(): Habit = Habit(
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
        try { DayOfWeek.valueOf(name) } catch (_: Exception) { null }
    }.toSet(),
    inactiveSinceTimestamp = inactiveSinceTimestamp,
    reminderVoice = reminderVoice
)

fun Habit.toEntity(): HabitEntity = HabitEntity(
    id = id,
    name = name,
    description = description,
    durationDays = durationDays,
    colorHex = colorHex,
    isActive = isActive,
    reminderTimes = reminderTimes,
    createdAt = createdAt,
    startedAt = startedAt,
    status = status.name,
    cycleStartDate = cycleStartDate,
    cycleEndDate = cycleEndDate,
    inactiveDaysCount = inactiveDaysCount,
    activeDays = activeDays.map { it.name },
    inactiveSinceTimestamp = inactiveSinceTimestamp,
    reminderVoice = reminderVoice
)

fun HabitLogEntity.toDomain(): HabitLog = HabitLog(
    id = id,
    habitId = habitId,
    logDate = logDate,
    completed = completed,
    state = state
)

fun HabitLog.toEntity(): HabitLogEntity = HabitLogEntity(
    id = id,
    habitId = habitId,
    logDate = logDate,
    completed = completed,
    state = state
)

fun HabitCycleHistoryEntity.toDomain(): HabitCycleHistory = HabitCycleHistory(
    id = id,
    habitId = habitId,
    cycleStartDate = cycleStartDate,
    cycleEndDate = cycleEndDate,
    completionPercentage = completionPercentage,
    result = result,
    logsSnapshot = logsSnapshot
)

fun HabitCycleHistory.toEntity(): HabitCycleHistoryEntity = HabitCycleHistoryEntity(
    id = id,
    habitId = habitId,
    cycleStartDate = cycleStartDate,
    cycleEndDate = cycleEndDate,
    completionPercentage = completionPercentage,
    result = result,
    logsSnapshot = logsSnapshot
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
