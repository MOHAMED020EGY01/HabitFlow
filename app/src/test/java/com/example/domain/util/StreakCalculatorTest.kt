package com.example.domain.util

import com.example.domain.model.HabitLog
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    @Test
    fun `empty logs return zero streak`() {
        val logs = emptyList<HabitLog>()
        val result = StreakCalculator.calculateStreak(logs)
        assertEquals(0, result)
    }

    @Test
    fun `streak is 1 when only today is completed`() {
        val today = LocalDate.now().toString()
        val logs = listOf(
            HabitLog(id = 1, habitId = 1, logDate = today, completed = true)
        )
        val result = StreakCalculator.calculateStreak(logs)
        assertEquals(1, result)
    }

    @Test
    fun `streak is 1 when only yesterday is completed`() {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val logs = listOf(
            HabitLog(id = 1, habitId = 1, logDate = yesterday, completed = true)
        )
        val result = StreakCalculator.calculateStreak(logs)
        assertEquals(1, result)
    }

    @Test
    fun `streak is 0 when today and yesterday are both missed`() {
        val today = LocalDate.now().toString()
        val yesterday = LocalDate.now().minusDays(1).toString()
        val dayBefore = LocalDate.now().minusDays(2).toString()
        val logs = listOf(
            HabitLog(id = 1, habitId = 1, logDate = today, completed = false),
            HabitLog(id = 1, habitId = 1, logDate = yesterday, completed = false),
            HabitLog(id = 1, habitId = 1, logDate = dayBefore, completed = true)
        )
        val result = StreakCalculator.calculateStreak(logs)
        assertEquals(0, result)
    }

    @Test
    fun `streak counts correctly for contiguous completed days ending today`() {
        val today = LocalDate.now()
        val logs = listOf(
            HabitLog(id = 1, habitId = 1, logDate = today.toString(), completed = true),
            HabitLog(id = 1, habitId = 1, logDate = today.minusDays(1).toString(), completed = true),
            HabitLog(id = 1, habitId = 1, logDate = today.minusDays(2).toString(), completed = true),
            HabitLog(id = 1, habitId = 1, logDate = today.minusDays(3).toString(), completed = false)
        )
        val result = StreakCalculator.calculateStreak(logs)
        assertEquals(3, result)
    }

    @Test
    fun `streak counts correctly for contiguous completed days ending yesterday`() {
        val today = LocalDate.now()
        val logs = listOf(
            HabitLog(id = 1, habitId = 1, logDate = today.toString(), completed = false),
            HabitLog(id = 1, habitId = 1, logDate = today.minusDays(1).toString(), completed = true),
            HabitLog(id = 1, habitId = 1, logDate = today.minusDays(2).toString(), completed = true),
            HabitLog(id = 1, habitId = 1, logDate = today.minusDays(3).toString(), completed = true)
        )
        val result = StreakCalculator.calculateStreak(logs)
        assertEquals(3, result)
    }

    /**
     * DEMONSTRATING BUG C1:
     * This test demonstrates the critical bug C1. StreakCalculator is expected to calculate the
     * longest historical streak of a habit, but currently it only calculates the current consecutive
     * streak starting backwards from LocalDate.now().
     *
     * In this scenario:
     * - The habit was completed for 10 consecutive days in the past (e.g. 20 days ago to 10 days ago).
     * - The habit was missed recently.
     * - The habit is currently on a 1-day active streak.
     *
     * Expected best/longest historical streak: 10 days.
     * Actual returned value from current StreakCalculator: 1 day (or 0 if not completed today/yesterday).
     */
    @Test
    fun `demonstrate_C1_historical_streak_bug`() {
        val today = LocalDate.now()
        
        // Let's build logs with a historical streak of 5 days, then a gap of 2 days, then current 1-day streak.
        val logs = listOf(
            // Current streak (1 day)
            HabitLog(id = 1, habitId = 1, logDate = today.toString(), completed = true),
            
            // Gap of 2 missed days
            HabitLog(id = 1, habitId = 1, logDate = today.minusDays(1).toString(), completed = false),
            HabitLog(id = 1, habitId = 1, logDate = today.minusDays(2).toString(), completed = false),
            
            // Historical streak (5 days)
            HabitLog(id = 1, habitId = 1, logDate = today.minusDays(3).toString(), completed = true),
            HabitLog(id = 1, habitId = 1, logDate = today.minusDays(4).toString(), completed = true),
            HabitLog(id = 1, habitId = 1, logDate = today.minusDays(5).toString(), completed = true),
            HabitLog(id = 1, habitId = 1, logDate = today.minusDays(6).toString(), completed = true),
            HabitLog(id = 1, habitId = 1, logDate = today.minusDays(7).toString(), completed = true)
        )

        val actualStreak = StreakCalculator.calculateStreak(logs)
        
        // Document the discrepancy:
        val expectedBestHistoricalStreak = 5
        
        println("BUG C1 DEMONSTRATION:")
        println("Expected best historical streak: $expectedBestHistoricalStreak")
        println("Actual calculated streak ending today/yesterday: $actualStreak")
        
        // This assertion confirms that StreakCalculator returns 1 instead of 5, demonstrating bug C1.
        assertEquals(1, actualStreak)
    }
}
