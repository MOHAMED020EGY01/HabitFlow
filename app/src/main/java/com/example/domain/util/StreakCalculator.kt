package com.example.domain.util

import com.example.domain.model.HabitLog
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

object StreakCalculator {

    private val streakCache = ConcurrentHashMap<Int, Pair<Int, Int>>()

    fun invalidateCache(habitId: Int) { streakCache.remove(habitId) }
    fun invalidateAll() { streakCache.clear() }

    fun calculateStreak(logs: List<HabitLog>): Int {
        val completedDates = logs.filter { it.completed }.map { it.logDate }.toSet()
        return calculateStreakFromDates(completedDates)
    }

    fun calculateStreakFromDates(completedDates: Set<String>): Int =
        calculateStreakFromDates(completedDates, activeDays = null, habitId = -1)

    /**
     * Calculates streak with active-days awareness.
     *
     * Days NOT in [activeDays] are skipped entirely — they neither count
     * toward the streak nor break it.
     *
     * ## Worked example
     * activeDays = {MON, WED, FRI}, today = Thursday 2026-07-10
     * Completed dates: Mon 7th, Wed 9th
     * Walk: Thu(skip)→Wed(completed→+1)→Tue(skip)→Mon(completed→+1)→Sun(skip)
     *       →Sat(skip)→Fri(not completed, streak>0)→break
     * Result: 2 (Mon + Wed)
     */
    fun calculateStreakFromDates(
        completedDates: Set<String>,
        activeDays: Set<DayOfWeek>?,
        habitId: Int = -1
    ): Int {
        val signature = if (activeDays != null) {
            completedDates.hashCode() * 31 + completedDates.size + activeDays.hashCode() * 7
        } else {
            completedDates.hashCode() * 31 + completedDates.size
        }
        val cached = streakCache[habitId]
        if (cached != null && cached.first == signature) return cached.second

        val result = if (activeDays != null) {
            computeStreak(completedDates, activeDays)
        } else {
            computeStreak(completedDates, null)
        }
        streakCache[habitId] = signature to result
        return result
    }

    fun calculateStreakFromDates(completedDates: Set<String>, habitId: Int): Int =
        calculateStreakFromDates(completedDates, activeDays = null, habitId = habitId)

    /**
     * Core streak algorithm.
     *
     * Walks backward from today.  Days NOT in [activeDays] are skipped.
     * For other days: if completed → increment streak and continue;
     * if not completed → if streak is 0, skip (look for streak start);
     * if streak > 0, break (streak broken by a missed scheduled day).
     */
    private fun computeStreak(
        completedDates: Set<String>,
        activeDays: Set<DayOfWeek>?
    ): Int {
        var streak = 0
        var checkDate = LocalDate.now()
        var foundStart = false

        // Allow at most 7 lookback days where streak==0 to find the start.
        // After that, if we still haven't found a completed day, exit.
        var emptyLookback = 0
        val maxEmptyLookback = if (activeDays != null) 14 else 3

        while (emptyLookback < maxEmptyLookback) {
            val dayOfWeek = checkDate.dayOfWeek

            // Skip non-active days
            if (activeDays != null && dayOfWeek !in activeDays) {
                checkDate = checkDate.minusDays(1)
                continue
            }

            val dateStr = checkDate.toString()
            if (completedDates.contains(dateStr)) {
                streak++
                foundStart = true
                emptyLookback = 0
                checkDate = checkDate.minusDays(1)
            } else {
                if (!foundStart) {
                    // Haven't found a streak yet — skip this missed day
                    emptyLookback++
                    checkDate = checkDate.minusDays(1)
                    continue
                }
                // Found a streak before, but this scheduled day is missed → break
                break
            }
        }
        return streak
    }
}
