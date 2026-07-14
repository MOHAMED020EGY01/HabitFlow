package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.HabitEntity
import com.example.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: Int): HabitEntity?

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    fun getHabitByIdFlow(id: Int): Flow<HabitEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId")
    fun getLogsForHabit(habitId: Int): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId")
    suspend fun getLogsForHabitSync(habitId: Int): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs")
    fun getAllLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs")
    suspend fun getAllLogsSync(): List<HabitLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLogEntity): Long

    @Update
    suspend fun updateLog(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND logDate = :logDate")
    suspend fun deleteLogForDate(habitId: Int, logDate: String)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    suspend fun deleteLogsForHabit(habitId: Int)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND logDate = :logDate LIMIT 1")
    suspend fun getLogForDate(habitId: Int, logDate: String): HabitLogEntity?

    @Query("SELECT COUNT(*) FROM habits WHERE isActive = 1")
    suspend fun getActiveHabitsCount(): Int

    @Query("SELECT COUNT(*) FROM habits WHERE isActive = 1")
    fun observeActiveHabitsCount(): Flow<Int>

    @Query("UPDATE habits SET isActive = :isActive, startedAt = :startedAt, status = :status, inactiveSinceTimestamp = :inactiveSince WHERE id = :habitId")
    suspend fun updateActiveStatus(habitId: Int, isActive: Boolean, startedAt: Long?, status: String, inactiveSince: Long?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycleHistory(history: com.example.data.local.entity.HabitCycleHistoryEntity): Long

    @Query("SELECT * FROM habit_cycle_history WHERE habitId = :habitId ORDER BY cycleEndDate DESC")
    fun getCycleHistoryForHabit(habitId: Int): Flow<List<com.example.data.local.entity.HabitCycleHistoryEntity>>

    @Query("SELECT * FROM habit_cycle_history WHERE habitId = :habitId ORDER BY cycleEndDate DESC")
    suspend fun getCycleHistoryForHabitSync(habitId: Int): List<com.example.data.local.entity.HabitCycleHistoryEntity>

    @Query("""
        SELECT h.*, 
               (SELECT COUNT(*) FROM habit_logs WHERE habitId = h.id AND completed = 1) AS completedDaysCount,
               EXISTS(SELECT 1 FROM habit_logs WHERE habitId = h.id AND logDate = :today AND completed = 1) AS isCompletedToday
        FROM habits h
        WHERE h.isActive = 1
        ORDER BY COALESCE(h.startedAt, h.createdAt) ASC
        LIMIT 6
    """)
    suspend fun getTopActiveHabitsForWidgets(today: String): List<HabitWidgetDisplayEntity>

    @Query("""
        SELECT h.*, 
               (SELECT COUNT(*) FROM habit_logs WHERE habitId = h.id AND completed = 1) AS completedCount,
               EXISTS(SELECT 1 FROM habit_logs WHERE habitId = h.id AND logDate = :today AND completed = 1) AS isCompletedToday
        FROM habits h
        ORDER BY h.createdAt DESC
    """)
    fun getAllHabitsWithCompletion(today: String): Flow<List<HabitWithCompletedCount>>

    @Query("SELECT habitId, logDate FROM habit_logs WHERE completed = 1")
    fun getCompletedLogDates(): Flow<List<HabitLogDateProj>>

    /** NEW: Only active habits, limited to 6, with pre-calculated completion data */
    @Query("""
        SELECT h.*, 
               (SELECT COUNT(*) FROM habit_logs WHERE habitId = h.id AND completed = 1) AS completedCount,
               EXISTS(SELECT 1 FROM habit_logs WHERE habitId = h.id AND logDate = :today AND completed = 1) AS isCompletedToday
        FROM habits h
        WHERE h.isActive = 1
        ORDER BY COALESCE(h.startedAt, h.createdAt) ASC
        LIMIT 6
    """)
    fun getActiveHabitsWithCompletion(today: String): Flow<List<HabitWithCompletedCount>>

    /** NEW: Returns completed date strings for streak calculation */
    @Query("""
        SELECT logDate FROM habit_logs 
        WHERE habitId = :habitId AND completed = 1 
        ORDER BY logDate DESC
    """)
    suspend fun getCompletedDateStringsForHabit(habitId: Int): List<String>

    /** NEW: Bulk insert for daily rollover optimization */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLogsBulk(logs: List<HabitLogEntity>)

    /** NEW: Lightweight reminder time queries */
    data class ReminderTimeWithHabitName(
        val name: String,
        val reminderTimes: String?
    )

    @Query("SELECT name, reminderTimes FROM habits")
    suspend fun getAllReminderTimesRaw(): List<ReminderTimeWithHabitName>

    @Query("SELECT name, reminderTimes FROM habits WHERE id != :excludeHabitId")
    suspend fun getReminderTimesExcluding(excludeHabitId: Int): List<ReminderTimeWithHabitName>

    @Query("SELECT * FROM habits WHERE status = 'INACTIVE' ORDER BY inactiveSinceTimestamp DESC LIMIT :limit")
    suspend fun getRecentInactiveHabits(limit: Int): List<HabitEntity>
}

data class HabitWidgetDisplayEntity(
    @Embedded val habit: HabitEntity,
    val completedDaysCount: Int,
    val isCompletedToday: Boolean
)

data class HabitWithCompletedCount(
    @Embedded val habit: HabitEntity,
    val completedCount: Int,
    val isCompletedToday: Boolean
)

data class HabitLogDateProj(
    val habitId: Int,
    val logDate: String
)


