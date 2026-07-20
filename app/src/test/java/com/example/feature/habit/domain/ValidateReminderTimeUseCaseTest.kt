package com.example.feature.habit.domain

import com.example.core.model.domain.Habit
import com.example.core.repository.FakeHabitRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ValidateReminderTimeUseCaseTest {

    private lateinit var repository: FakeHabitRepository
    private lateinit var validateReminderTimeUseCase: ValidateReminderTimeUseCase

    @Before
    fun setUp() {
        repository = FakeHabitRepository()
        validateReminderTimeUseCase = ValidateReminderTimeUseCase(repository)
    }

    private fun createTestHabit(
        id: Int = 0,
        name: String = "Test Habit",
        description: String = "Test Description",
        durationDays: Int = 30,
        colorHex: String = "#E53935",
        isActive: Boolean = true,
        reminderTimes: List<String> = listOf("09:00"),
        activeDays: Set<java.time.DayOfWeek> = java.time.DayOfWeek.values().toSet(),
        createdAt: Long = System.currentTimeMillis(),
        startedAt: Long? = System.currentTimeMillis()
    ) = Habit(
        id = id,
        name = name,
        description = description,
        durationDays = durationDays,
        colorHex = colorHex,
        isActive = isActive,
        reminderTimes = reminderTimes,
        activeDays = activeDays,
        createdAt = createdAt,
        startedAt = startedAt
    )

    @Test
    fun `no conflict when database is empty`() = runTest {
        val result = validateReminderTimeUseCase.validate("08:00", java.time.DayOfWeek.values().toSet())
        assertTrue(result is ValidateReminderTimeUseCase.ValidationResult.Valid)
    }

    @Test
    fun `exact 10 minute gap boundary does NOT conflict`() = runTest {
        // Given a habit with reminder at 08:00
        repository.insertHabit(createTestHabit(id = 1, name = "Habit A", isActive = true, reminderTimes = listOf("08:00")))

        // When validating 08:10 (exact 10 min gap)
        val result1 = validateReminderTimeUseCase.validate("08:10", java.time.DayOfWeek.values().toSet())
        // When validating 07:50 (exact 10 min gap before)
        val result2 = validateReminderTimeUseCase.validate("07:50", java.time.DayOfWeek.values().toSet())

        // Then both are valid
        assertTrue(result1 is ValidateReminderTimeUseCase.ValidationResult.Valid)
        assertTrue(result2 is ValidateReminderTimeUseCase.ValidationResult.Valid)
    }

    @Test
    fun `less than 10 minute gap conflicts`() = runTest {
        // Given a habit with reminder at 08:00
        repository.insertHabit(createTestHabit(id = 1, name = "Habit A", isActive = true, reminderTimes = listOf("08:00")))

        // When validating 08:09 (9 minutes gap)
        val result1 = validateReminderTimeUseCase.validate("08:09", java.time.DayOfWeek.values().toSet())
        // When validating 07:51 (9 minutes gap)
        val result2 = validateReminderTimeUseCase.validate("07:51", java.time.DayOfWeek.values().toSet())

        // Then both conflict
        assertTrue(result1 is ValidateReminderTimeUseCase.ValidationResult.Conflict)
        assertTrue(result2 is ValidateReminderTimeUseCase.ValidationResult.Conflict)

        val conflict1 = result1 as ValidateReminderTimeUseCase.ValidationResult.Conflict
        assertEquals("Habit A", conflict1.conflictingHabitName)
        assertEquals("08:00", conflict1.conflictingTime)
    }

    @Test
    fun `no conflict when active days do not overlap`() = runTest {
        // Given a habit with reminder at 08:00 on Mondays
        repository.insertHabit(createTestHabit(id = 1, name = "Habit A", isActive = true, reminderTimes = listOf("08:00"), activeDays = setOf(java.time.DayOfWeek.MONDAY)))

        // When validating 08:00 on Tuesdays
        val result = validateReminderTimeUseCase.validate(
            proposedTime = "08:00",
            proposedActiveDays = setOf(java.time.DayOfWeek.TUESDAY)
        )

        // Then it is valid
        assertTrue(result is ValidateReminderTimeUseCase.ValidationResult.Valid)
    }

    @Test
    fun `conflict when active days overlap`() = runTest {
        // Given a habit with reminder at 08:00 on Mon/Wed
        repository.insertHabit(createTestHabit(id = 1, name = "Habit A", isActive = true, reminderTimes = listOf("08:00"), activeDays = setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY)))

        // When validating 08:05 on Wed/Fri
        val result = validateReminderTimeUseCase.validate(
            proposedTime = "08:05",
            proposedActiveDays = setOf(java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.FRIDAY)
        )

        // Then it conflicts because they both fire on Wednesday
        assertTrue(result is ValidateReminderTimeUseCase.ValidationResult.Conflict)
    }

    @Test
    fun `midnight wraparound conflicts within 10 minutes`() = runTest {
        // Given a habit with reminder at 23:55
        repository.insertHabit(createTestHabit(id = 1, name = "Late Habit", isActive = true, reminderTimes = listOf("23:55")))

        // When validating 00:04 (only 9 minutes gap across midnight)
        val result = validateReminderTimeUseCase.validate("00:04", java.time.DayOfWeek.values().toSet())

        // Then it conflicts
        assertTrue(result is ValidateReminderTimeUseCase.ValidationResult.Conflict)
        val conflict = result as ValidateReminderTimeUseCase.ValidationResult.Conflict
        assertEquals("Late Habit", conflict.conflictingHabitName)
        assertEquals("23:55", conflict.conflictingTime)
    }

    @Test
    fun `conflict with another reminder of the same habit`() = runTest {
        // When validating 08:05 for a habit that already has 08:00 in its list
        val result = validateReminderTimeUseCase.validate(
            proposedTime = "08:05",
            proposedActiveDays = setOf(java.time.DayOfWeek.MONDAY),
            otherTimesOfHabit = listOf("08:00")
        )

        // Then it conflicts
        assertTrue(result is ValidateReminderTimeUseCase.ValidationResult.Conflict)
        val conflict = result as ValidateReminderTimeUseCase.ValidationResult.Conflict
        assertEquals("this habit", conflict.conflictingHabitName)
        assertEquals("08:00", conflict.conflictingTime)
    }

    @Test
    fun `inactive habit does not block`() = runTest {
        // Given an INACTIVE habit with reminder 08:00
        repository.insertHabit(createTestHabit(id = 1, name = "Habit A", isActive = false, reminderTimes = listOf("08:00")))

        // When validating 08:05
        val result = validateReminderTimeUseCase.validate("08:05", java.time.DayOfWeek.values().toSet())

        // Then it is valid
        assertTrue(result is ValidateReminderTimeUseCase.ValidationResult.Valid)
    }

    @Test
    fun `editing own habit excludes it from repository check but includes local times`() = runTest {
        // Given a habit with id 1 and reminder 08:00 in DB
        repository.insertHabit(createTestHabit(id = 1, name = "Habit A", isActive = true, reminderTimes = listOf("08:00")))

        // When validating 08:05 while editing habit with id 1, BUT providing a local 08:02
        val result = validateReminderTimeUseCase.validate(
            proposedTime = "08:05",
            proposedActiveDays = java.time.DayOfWeek.values().toSet(),
            editingHabitId = 1,
            otherTimesOfHabit = listOf("08:02")
        )

        // Then it conflicts with the local "this habit" 08:02, NOT the DB 08:00
        assertTrue(result is ValidateReminderTimeUseCase.ValidationResult.Conflict)
        val conflict = result as ValidateReminderTimeUseCase.ValidationResult.Conflict
        assertEquals("this habit", conflict.conflictingHabitName)
        assertEquals("08:02", conflict.conflictingTime)
    }

    private fun assertEquals(expected: Any?, actual: Any?) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
