package com.example.domain.repository

import com.example.domain.model.Habit
import com.example.domain.model.HabitLog
import com.example.domain.model.HabitLogDate
import com.example.domain.model.HabitWithCompletion
import com.example.domain.model.HabitWithWidgetInfo
import com.example.domain.model.HabitCycleHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeHabitRepository : HabitRepository {
    private val habitsMap = mutableMapOf<Int, Habit>()
    private val logsList = mutableListOf<HabitLog>()
    private val cycleHistoryList = mutableListOf<HabitCycleHistory>()

    private val habitsFlow = MutableStateFlow<List<Habit>>(emptyList())
    private val logsFlow = MutableStateFlow<List<HabitLog>>(emptyList())

    private fun updateFlows() {
        habitsFlow.value = habitsMap.values.toList()
        logsFlow.value = logsList.toList()
    }

    override fun getAllHabits(): Flow<List<Habit>> = habitsFlow

    override suspend fun getAllHabitsSync(): List<Habit> = habitsMap.values.toList()

    override suspend fun getHabitById(id: Int): Habit? = habitsMap[id]

    override fun getHabitByIdFlow(id: Int): Flow<Habit?> = habitsFlow.map { list -> list.find { it.id == id } }

    override suspend fun insertHabit(habit: Habit): Int {
        val id = if (habit.id == 0) (habitsMap.keys.maxOrNull() ?: 0) + 1 else habit.id
        val newHabit = habit.copy(id = id)
        habitsMap[id] = newHabit
        updateFlows()
        return id
    }

    override suspend fun updateHabit(habit: Habit) {
        habitsMap[habit.id] = habit
        updateFlows()
    }

    override suspend fun deleteHabit(habit: Habit) {
        habitsMap.remove(habit.id)
        updateFlows()
    }

    override fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>> = logsFlow.map { list -> list.filter { it.habitId == habitId } }

    override suspend fun getLogsForHabitSync(habitId: Int): List<HabitLog> = logsList.filter { it.habitId == habitId }

    override fun getAllLogs(): Flow<List<HabitLog>> = logsFlow

    override suspend fun getAllLogsSync(): List<HabitLog> = logsList.toList()

    override suspend fun insertLog(log: HabitLog) {
        logsList.removeIf { it.habitId == log.habitId && it.logDate == log.logDate }
        logsList.add(log)
        updateFlows()
    }

    override suspend fun insertLogsBulk(logs: List<HabitLog>) {
        logs.forEach { log ->
            logsList.removeIf { it.habitId == log.habitId && it.logDate == log.logDate }
            logsList.add(log)
        }
        updateFlows()
    }

    override suspend fun toggleLogForDate(habitId: Int, logDate: String, completed: Boolean) {
        logHabitCompletion(habitId, logDate, completed)
    }

    override suspend fun logHabitCompletion(habitId: Int, date: String, completed: Boolean) {
        logsList.removeIf { it.habitId == habitId && it.logDate == date }
        logsList.add(HabitLog(id = 0, habitId = habitId, logDate = date, completed = completed))
        updateFlows()
    }

    override suspend fun getCompletedDaysCount(habitId: Int): Int {
        return logsList.count { it.habitId == habitId && it.completed }
    }

    override suspend fun getActiveHabitsCount(): Int {
        return habitsMap.values.count { it.isActive }
    }

    override fun observeActiveHabitsCount(): Flow<Int> = habitsFlow.map { list -> list.count { it.isActive } }

    override suspend fun setHabitActive(habitId: Int, isActive: Boolean, startedAt: Long?) {
        val habit = habitsMap[habitId]
        if (habit != null) {
            habitsMap[habitId] = habit.copy(isActive = isActive, startedAt = startedAt)
            updateFlows()
        }
    }

    override fun getCycleHistoryForHabit(habitId: Int): Flow<List<HabitCycleHistory>> = MutableStateFlow(cycleHistoryList.filter { it.habitId == habitId })

    override suspend fun getCycleHistoryForHabitSync(habitId: Int): List<HabitCycleHistory> = cycleHistoryList.filter { it.habitId == habitId }

    override suspend fun insertCycleHistory(history: HabitCycleHistory) {
        cycleHistoryList.add(history)
    }

    override suspend fun deleteLogsForHabit(habitId: Int) {
        logsList.removeIf { it.habitId == habitId }
        updateFlows()
    }

    override suspend fun getTopActiveHabitsForWidgets(today: String): List<HabitWithWidgetInfo> {
        return habitsMap.values.filter { it.isActive }.take(3).map {
            HabitWithWidgetInfo(
                habitId = it.id,
                name = it.name,
                colorHex = it.colorHex,
                daysCompleted = 0,
                totalDays = it.durationDays,
                daysRemaining = it.durationDays,
                progressPercent = 0f,
                isCompletedToday = false,
                reminderTimes = it.reminderTimes
            )
        }
    }

    override fun getAllHabitsWithCompletion(today: String): Flow<List<HabitWithCompletion>> {
        return habitsFlow.map { list ->
            list.map { h ->
                val logs = logsList.filter { it.habitId == h.id }
                val completedToday = logs.any { it.logDate == today && it.completed }
                val completedCount = logs.count { it.completed }
                HabitWithCompletion(h, completedCount = completedCount, isCompletedToday = completedToday)
            }
        }
    }

    override fun getActiveHabitsWithCompletion(today: String): Flow<List<HabitWithCompletion>> {
        return habitsFlow.map { list ->
            list.filter { it.isActive }.map { h ->
                val logs = logsList.filter { it.habitId == h.id }
                val completedToday = logs.any { it.logDate == today && it.completed }
                val completedCount = logs.count { it.completed }
                HabitWithCompletion(h, completedCount = completedCount, isCompletedToday = completedToday)
            }
        }
    }

    override suspend fun getCompletedDateStringsForHabit(habitId: Int): List<String> {
        return logsList.filter { it.habitId == habitId && it.completed }.map { it.logDate }
    }

    override suspend fun getAllReminderTimes(excludeHabitId: Int?): List<ReminderTimeEntry> {
        return habitsMap.values
            .filter { it.id != excludeHabitId }
            .flatMap { habit ->
                habit.reminderTimes.map { time ->
                    ReminderTimeEntry(habit.name, time)
                }
            }
    }

    override fun getCompletedLogDates(): Flow<List<HabitLogDate>> {
        return logsFlow.map { list ->
            list.filter { it.completed }.map { HabitLogDate(it.habitId, it.logDate) }
        }
    }

    override fun getAllNotificationsFlow(): Flow<List<com.example.domain.model.HabitNotification>> {
        return MutableStateFlow(emptyList())
    }

    override suspend fun insertNotification(notification: com.example.domain.model.HabitNotification) {}

    override suspend fun deleteNotificationById(id: Int) {}

    override suspend fun deleteAllNotifications() {}
}
