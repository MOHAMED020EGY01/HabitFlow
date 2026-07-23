package com.example.core.repository

import com.example.core.model.domain.Habit
import com.example.core.model.domain.HabitLog
import kotlinx.coroutines.flow.Flow

/** A single reminder time with the habit name it belongs to */
data class ReminderTimeEntry(
    val habitName: String,
    val time: String
)

interface HabitRepository {
    fun getAllHabits(): Flow<List<Habit>>
    suspend fun getAllHabitsSync(): List<Habit>
    suspend fun getHabitById(id: Int): Habit?
    fun getHabitByIdFlow(id: Int): Flow<Habit?>
    suspend fun insertHabit(habit: Habit): Int
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(habit: Habit)

    fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>>
    suspend fun getLogsForHabitSync(habitId: Int): List<HabitLog>
    fun getAllLogs(): Flow<List<HabitLog>>
    suspend fun getAllLogsSync(): List<HabitLog>
    suspend fun insertLog(log: HabitLog)
    suspend fun insertLogsBulk(logs: List<HabitLog>)
    suspend fun toggleLogForDate(habitId: Int, logDate: String, completed: Boolean)
    suspend fun logHabitCompletion(habitId: Int, date: String, completed: Boolean)
    suspend fun getCompletedDaysCount(habitId: Int): Int
    suspend fun getCompletedCountForCycle(habitId: Int, startDate: String): Int
    suspend fun getActiveHabitsCount(): Int
    fun observeActiveHabitsCount(): Flow<Int>
    suspend fun setHabitActive(habitId: Int, isActive: Boolean, startedAt: Long?, status: com.example.core.model.domain.HabitStatus, inactiveSince: Long?)

    fun getCycleHistoryForHabit(habitId: Int): Flow<List<com.example.core.model.domain.HabitCycleHistory>>
    suspend fun getCycleHistoryForHabitSync(habitId: Int): List<com.example.core.model.domain.HabitCycleHistory>
    suspend fun insertCycleHistory(history: com.example.core.model.domain.HabitCycleHistory)
    suspend fun deleteLogsForHabit(habitId: Int)
    suspend fun getTopActiveHabitsForWidgets(today: String): List<com.example.core.model.domain.HabitWithWidgetInfo>
    fun getAllHabitsWithCompletion(today: String): Flow<List<com.example.core.model.domain.HabitWithCompletion>>
    fun getCompletedLogDates(): Flow<List<com.example.core.model.domain.HabitLogDate>>

    suspend fun isHabitCompletedToday(habitId: Int): Boolean

    // === NEW LIGHTWEIGHT METHODS ===
    /** Only active top-6 habits with pre-calculated completion data */
    fun getActiveHabitsWithCompletion(today: String): Flow<List<com.example.core.model.domain.HabitWithCompletion>>

    /** Returns completed date strings for streak calculation */
    suspend fun getCompletedDateStringsForHabit(habitId: Int): List<String>

    /** Returns flattened reminder times with habit names for conflict validation */
    suspend fun getAllReminderTimes(excludeHabitId: Int? = null): List<ReminderTimeEntry>

    /** Returns up to 'limit' inactive habits ordered by most-recent-inactivation first */
    suspend fun getRecentInactiveHabits(limit: Int): List<Habit>

    // === NOTIFICATIONS ===
    fun getAllNotificationsFlow(): Flow<List<com.example.core.model.domain.HabitNotification>>
    suspend fun insertNotification(notification: com.example.core.model.domain.HabitNotification)
    suspend fun deleteNotificationById(id: Int)
    suspend fun deleteAllNotifications()
}

