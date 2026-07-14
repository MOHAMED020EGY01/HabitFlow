package com.example.domain.usecase

import com.example.domain.model.ActivationResult
import com.example.domain.model.Habit
import com.example.domain.repository.FakeHabitRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ToggleHabitActiveUseCaseTest {

    private lateinit var repository: FakeHabitRepository
    private lateinit var toggleHabitActiveUseCase: ToggleHabitActiveUseCase

    @Before
    fun setUp() {
        repository = FakeHabitRepository()
        toggleHabitActiveUseCase = ToggleHabitActiveUseCase(repository)
    }

    private fun createTestHabit(
        id: Int = 0,
        name: String = "Test Habit",
        description: String = "Test Description",
        durationDays: Int = 30,
        colorHex: String = "#E53935",
        isActive: Boolean = true,
        reminderTimes: List<String> = listOf("09:00"),
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
        createdAt = createdAt,
        startedAt = startedAt
    )

    @Test
    fun `deactivating active habit returns NotApplicable and updates database`() = runTest {
        // Given an active habit
        val habitId = repository.insertHabit(createTestHabit(name = "Habit 1", isActive = true, reminderTimes = listOf("08:00")))

        // When deactivating it
        val result = toggleHabitActiveUseCase(habitId, makeActive = false)

        // Then it returns NotApplicable and updates state in database to false
        assertEquals(ActivationResult.NotApplicable, result)
        val updated = repository.getHabitById(habitId)
        assertFalse(updated!!.isActive)
    }

    @Test
    fun `activating inactive habit succeeds if active count is less than 6`() = runTest {
        // Given 5 active habits and 1 inactive habit
        repeat(5) { i ->
            repository.insertHabit(createTestHabit(name = "Active $i", isActive = true, reminderTimes = listOf("08:00")))
        }
        val inactiveId = repository.insertHabit(createTestHabit(name = "Inactive", isActive = false, reminderTimes = listOf("10:00"), startedAt = null))

        // When activating the inactive habit
        val result = toggleHabitActiveUseCase(inactiveId, makeActive = true)

        // Then it succeeds as Activated
        assertEquals(ActivationResult.Activated, result)
        val updated = repository.getHabitById(inactiveId)
        assertTrue(updated!!.isActive)
    }

    @Test
    fun `activating inactive habit fails if active count is already 6`() = runTest {
        // Given 6 active habits and 1 inactive habit
        repeat(6) { i ->
            repository.insertHabit(createTestHabit(name = "Active $i", isActive = true, reminderTimes = listOf("08:00")))
        }
        val inactiveId = repository.insertHabit(createTestHabit(name = "Inactive", isActive = false, reminderTimes = listOf("11:00"), startedAt = null))

        // When activating the inactive habit
        val result = toggleHabitActiveUseCase(inactiveId, makeActive = true)

        // Then it fails returning SavedAsInactive
        assertTrue(result is ActivationResult.SavedAsInactive)
        assertEquals(6, (result as ActivationResult.SavedAsInactive).currentActiveCount)
        val updated = repository.getHabitById(inactiveId)
        assertFalse(updated!!.isActive) // Remains inactive
    }
}
