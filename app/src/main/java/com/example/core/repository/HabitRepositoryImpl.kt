package com.example.core.repository

import com.example.core.database.dao.HabitDao
import com.example.core.database.dao.NotificationDao
import com.example.core.model.entity.HabitEntity
import com.example.core.model.entity.HabitLogEntity
import com.example.core.model.mapper.toDomain
import com.example.core.model.mapper.toEntity
import com.example.core.model.domain.Habit

import com.example.core.model.domain.HabitLog
import com.example.core.model.domain.HabitNotification
import com.example.core.repository.HabitRepository
import com.example.core.repository.ReminderTimeEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val notificationDao: NotificationDao
) : HabitRepository {
    override fun getAllHabits(): Flow<List<Habit>> {
        return habitDao.getAllHabits().map { entities ->
            entities.map { it.toDomain() }
        }.distinctUntilChanged()
    }

    override suspend fun getAllHabitsSync(): List<Habit> {
        return getAllHabits().first()
    }

    override suspend fun getHabitById(id: Int): Habit? {
        return habitDao.getHabitById(id)?.toDomain()
    }

    override fun getHabitByIdFlow(id: Int): Flow<Habit?> {
        return habitDao.getHabitByIdFlow(id).map { it?.toDomain() }.distinctUntilChanged()
    }

    override suspend fun insertHabit(habit: Habit): Int {
        return habitDao.insertHabit(habit.toEntity()).toInt()
    }

    override suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit.toEntity())
    }

    override suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit.toEntity())
    }

    override fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>> {
        return habitDao.getLogsForHabit(habitId).map { entities ->
            entities.map { it.toDomain() }
        }.distinctUntilChanged()
    }

    override suspend fun getLogsForHabitSync(habitId: Int): List<HabitLog> {
        return habitDao.getLogsForHabitSync(habitId).map { it.toDomain() }
    }

    override fun getAllLogs(): Flow<List<HabitLog>> {
        return habitDao.getAllLogs().map { entities ->
            entities.map { it.toDomain() }
        }.distinctUntilChanged()
    }

    override suspend fun getAllLogsSync(): List<HabitLog> {
        return habitDao.getAllLogsSync().map { it.toDomain() }
    }

    override suspend fun insertLog(log: HabitLog) {
        habitDao.insertLog(log.toEntity())
    }

    override suspend fun insertLogsBulk(logs: List<HabitLog>) {
        val entities = logs.map { it.toEntity() }
        habitDao.insertLogsBulk(entities)
    }

    override suspend fun toggleLogForDate(habitId: Int, logDate: String, completed: Boolean) {
        if (completed) {
            val existing = habitDao.getLogForDate(habitId, logDate)
            if (existing == null) {
                habitDao.insertLog(HabitLogEntity(habitId = habitId, logDate = logDate, completed = true))
            }
        } else {
            habitDao.deleteLogForDate(habitId, logDate)
        }
    }

    override suspend fun logHabitCompletion(habitId: Int, date: String, completed: Boolean) {
        val existing = habitDao.getLogForDate(habitId, date)
        if (existing != null) {
            habitDao.updateLog(existing.copy(completed = completed))
        } else {
            habitDao.insertLog(
                HabitLogEntity(
                    habitId   = habitId,
                    logDate   = date,
                    completed = completed
                )
            )
        }
    }

    override suspend fun getCompletedDaysCount(habitId: Int): Int {
        return getLogsForHabitSync(habitId).count { it.completed }
    }

    override suspend fun getActiveHabitsCount(): Int {
        return habitDao.getActiveHabitsCount()
    }

    override fun observeActiveHabitsCount(): Flow<Int> {
        return habitDao.observeActiveHabitsCount().distinctUntilChanged()
    }

    override suspend fun setHabitActive(habitId: Int, isActive: Boolean, startedAt: Long?, status: com.example.core.model.domain.HabitStatus, inactiveSince: Long?) {
        habitDao.updateActiveStatus(habitId, isActive, startedAt, status.name, inactiveSince)
    }

    override fun getCycleHistoryForHabit(habitId: Int): Flow<List<com.example.core.model.domain.HabitCycleHistory>> {
        return habitDao.getCycleHistoryForHabit(habitId).map { entities ->
            entities.map { it.toDomain() }
        }.distinctUntilChanged()
    }

    override suspend fun getCycleHistoryForHabitSync(habitId: Int): List<com.example.core.model.domain.HabitCycleHistory> {
        return habitDao.getCycleHistoryForHabitSync(habitId).map { it.toDomain() }
    }

    override suspend fun insertCycleHistory(history: com.example.core.model.domain.HabitCycleHistory) {
        habitDao.insertCycleHistory(history.toEntity())
    }

    override suspend fun deleteLogsForHabit(habitId: Int) {
        habitDao.deleteLogsForHabit(habitId)
    }

    override suspend fun getTopActiveHabitsForWidgets(today: String): List<com.example.core.model.domain.HabitWithWidgetInfo> {
        return habitDao.getTopActiveHabitsForWidgets(today).map { displayEntity ->
            val habit = displayEntity.habit.toDomain()
            val completed = displayEntity.completedDaysCount
            val total = habit.getScheduledDaysCount()
            val remaining = (total - completed).coerceAtLeast(0)
            val percent = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0f
            com.example.core.model.domain.HabitWithWidgetInfo(
                habitId = habit.id,
                name = habit.name,
                colorHex = habit.colorHex,
                daysCompleted = completed,
                totalDays = total,
                daysRemaining = remaining,
                progressPercent = percent,
                isCompletedToday = displayEntity.isCompletedToday,
                reminderTimes = habit.reminderTimes,
                activeDays = habit.activeDays
            )
        }
    }

    override fun getAllHabitsWithCompletion(today: String): Flow<List<com.example.core.model.domain.HabitWithCompletion>> {
        return habitDao.getAllHabitsWithCompletion(today).map { entities ->
            entities.map { entity ->
                com.example.core.model.domain.HabitWithCompletion(
                    habit = entity.habit.toDomain(),
                    completedCount = entity.completedCount,
                    isCompletedToday = entity.isCompletedToday
                )
            }
        }.distinctUntilChanged()
    }

    override fun getCompletedLogDates(): Flow<List<com.example.core.model.domain.HabitLogDate>> {
        return habitDao.getCompletedLogDates().map { entities ->
            entities.map { entity ->
                com.example.core.model.domain.HabitLogDate(
                    habitId = entity.habitId,
                    logDate = entity.logDate
                )
            }
        }.distinctUntilChanged()
    }

    // === NEW LIGHTWEIGHT METHODS ===

    override fun getActiveHabitsWithCompletion(today: String): Flow<List<com.example.core.model.domain.HabitWithCompletion>> {
        return habitDao.getActiveHabitsWithCompletion(today).map { entities ->
            entities.map { entity ->
                com.example.core.model.domain.HabitWithCompletion(
                    habit = entity.habit.toDomain(),
                    completedCount = entity.completedCount,
                    isCompletedToday = entity.isCompletedToday
                )
            }
        }.distinctUntilChanged()
    }

    override suspend fun getCompletedDateStringsForHabit(habitId: Int): List<String> {
        return habitDao.getCompletedDateStringsForHabit(habitId)
    }

    override suspend fun getAllReminderTimes(excludeHabitId: Int?): List<ReminderTimeEntry> {
        val raw = if (excludeHabitId != null) {
            habitDao.getReminderTimesExcluding(excludeHabitId)
        } else {
            habitDao.getAllReminderTimesRaw()
        }
        return raw.flatMap { (name, csvTimes) ->
            if (csvTimes.isNullOrEmpty()) emptyList()
            else csvTimes.split(",").map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { ReminderTimeEntry(habitName = name, time = it) }
        }
    }

    override suspend fun getRecentInactiveHabits(limit: Int): List<Habit> {
        return habitDao.getRecentInactiveHabits(limit).map { it.toDomain() }
    }

    override fun getAllNotificationsFlow(): Flow<List<HabitNotification>> {
        return notificationDao.getAllNotificationsFlow().map { entities ->
            entities.map { it.toDomain() }
        }.distinctUntilChanged()
    }

    override suspend fun insertNotification(notification: HabitNotification) {
        notificationDao.insertNotification(notification.toEntity())
    }

    override suspend fun deleteNotificationById(id: Int) {
        notificationDao.deleteNotificationById(id)
    }

    override suspend fun deleteAllNotifications() {
        notificationDao.deleteAllNotifications()
    }
}

