package com.example.domain.repository

import com.example.domain.model.Habit
import com.example.domain.model.HabitLog
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
    suspend fun getActiveHabitsCount(): Int
    fun observeActiveHabitsCount(): Flow<Int>
    suspend fun setHabitActive(habitId: Int, isActive: Boolean, startedAt: Long?, status: com.example.domain.model.HabitStatus, inactiveSince: Long?)

    fun getCycleHistoryForHabit(habitId: Int): Flow<List<com.example.domain.model.HabitCycleHistory>>
    suspend fun getCycleHistoryForHabitSync(habitId: Int): List<com.example.domain.model.HabitCycleHistory>
    suspend fun insertCycleHistory(history: com.example.domain.model.HabitCycleHistory)
    suspend fun deleteLogsForHabit(habitId: Int)
    suspend fun getTopActiveHabitsForWidgets(today: String): List<com.example.domain.model.HabitWithWidgetInfo>
    fun getAllHabitsWithCompletion(today: String): Flow<List<com.example.domain.model.HabitWithCompletion>>
    fun getCompletedLogDates(): Flow<List<com.example.domain.model.HabitLogDate>>

    // === NEW LIGHTWEIGHT METHODS ===
    /** Only active top-6 habits with pre-calculated completion data */
    fun getActiveHabitsWithCompletion(today: String): Flow<List<com.example.domain.model.HabitWithCompletion>>

    /** Returns completed date strings for streak calculation */
    suspend fun getCompletedDateStringsForHabit(habitId: Int): List<String>

    /** Returns flattened reminder times with habit names for conflict validation */
    suspend fun getAllReminderTimes(excludeHabitId: Int? = null): List<ReminderTimeEntry>

    /** Returns up to 'limit' inactive habits ordered by most-recent-inactivation first */
    suspend fun getRecentInactiveHabits(limit: Int): List<Habit>

    // === NOTIFICATIONS ===
    fun getAllNotificationsFlow(): Flow<List<com.example.domain.model.HabitNotification>>
    suspend fun insertNotification(notification: com.example.domain.model.HabitNotification)
    suspend fun deleteNotificationById(id: Int)
    suspend fun deleteAllNotifications()
}

