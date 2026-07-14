package com.example.data.repository

import com.example.data.local.dao.HabitDao
import com.example.data.local.dao.NotificationDao
import com.example.data.local.entity.HabitEntity
import com.example.data.local.entity.HabitLogEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.model.Habit
import com.example.domain.model.HabitLog
import com.example.domain.model.HabitNotification
import com.example.domain.repository.HabitRepository
import com.example.domain.repository.ReminderTimeEntry
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
        return habitDao.insertHabit(HabitEntity.fromDomain(habit)).toInt()
    }

    override suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(HabitEntity.fromDomain(habit))
    }

    override suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(HabitEntity.fromDomain(habit))
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
        habitDao.insertLog(HabitLogEntity.fromDomain(log))
    }

    override suspend fun insertLogsBulk(logs: List<HabitLog>) {
        val entities = logs.map { HabitLogEntity.fromDomain(it) }
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

    override suspend fun setHabitActive(habitId: Int, isActive: Boolean, startedAt: Long?, status: com.example.domain.model.HabitStatus, inactiveSince: Long?) {
        habitDao.updateActiveStatus(habitId, isActive, startedAt, status.name, inactiveSince)
    }

    override fun getCycleHistoryForHabit(habitId: Int): Flow<List<com.example.domain.model.HabitCycleHistory>> {
        return habitDao.getCycleHistoryForHabit(habitId).map { entities ->
            entities.map { it.toDomain() }
        }.distinctUntilChanged()
    }

    override suspend fun getCycleHistoryForHabitSync(habitId: Int): List<com.example.domain.model.HabitCycleHistory> {
        return habitDao.getCycleHistoryForHabitSync(habitId).map { it.toDomain() }
    }

    override suspend fun insertCycleHistory(history: com.example.domain.model.HabitCycleHistory) {
        habitDao.insertCycleHistory(com.example.data.local.entity.HabitCycleHistoryEntity.fromDomain(history))
    }

    override suspend fun deleteLogsForHabit(habitId: Int) {
        habitDao.deleteLogsForHabit(habitId)
    }

    override suspend fun getTopActiveHabitsForWidgets(today: String): List<com.example.domain.model.HabitWithWidgetInfo> {
        return habitDao.getTopActiveHabitsForWidgets(today).map { displayEntity ->
            val habit = displayEntity.habit.toDomain()
            val completed = displayEntity.completedDaysCount
            val total = habit.getScheduledDaysCount()
            val remaining = (total - completed).coerceAtLeast(0)
            val percent = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0f
            com.example.domain.model.HabitWithWidgetInfo(
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

    override fun getAllHabitsWithCompletion(today: String): Flow<List<com.example.domain.model.HabitWithCompletion>> {
        return habitDao.getAllHabitsWithCompletion(today).map { entities ->
            entities.map { entity ->
                com.example.domain.model.HabitWithCompletion(
                    habit = entity.habit.toDomain(),
                    completedCount = entity.completedCount,
                    isCompletedToday = entity.isCompletedToday
                )
            }
        }.distinctUntilChanged()
    }

    override fun getCompletedLogDates(): Flow<List<com.example.domain.model.HabitLogDate>> {
        return habitDao.getCompletedLogDates().map { entities ->
            entities.map { entity ->
                com.example.domain.model.HabitLogDate(
                    habitId = entity.habitId,
                    logDate = entity.logDate
                )
            }
        }.distinctUntilChanged()
    }

    // === NEW LIGHTWEIGHT METHODS ===

    override fun getActiveHabitsWithCompletion(today: String): Flow<List<com.example.domain.model.HabitWithCompletion>> {
        return habitDao.getActiveHabitsWithCompletion(today).map { entities ->
            entities.map { entity ->
                com.example.domain.model.HabitWithCompletion(
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

