package com.example.feature.habit.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.domain.ActivationResult
import com.example.core.model.domain.Habit
import com.example.core.repository.FakeHabitRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AddHabitUseCaseTest {

    private lateinit var repository: FakeHabitRepository
    private lateinit var addHabitUseCase: AddHabitUseCase

    @Before
    fun setUp() {
        repository = FakeHabitRepository()
        addHabitUseCase = AddHabitUseCase(repository, ApplicationProvider.getApplicationContext())
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
    fun `add inactive habit always succeeds even if active count is at limit`() = runTest {
        // Given 6 active habits already in repository
        repeat(6) { i ->
            repository.insertHabit(createTestHabit(name = "Habit $i", isActive = true, reminderTimes = listOf("08:00")))
        }

        // When inserting an inactive habit
        val inactiveHabit = createTestHabit(name = "Habit 7", isActive = false, reminderTimes = listOf("11:00"), startedAt = null)
        val (result, savedHabit) = addHabitUseCase(inactiveHabit)

        // Then it succeeds and returns NotApplicable
        assertEquals(ActivationResult.NotApplicable, result)
        assertFalse(savedHabit.isActive)
        assertEquals(7, repository.getAllHabitsSync().size)
    }

    @Test
    fun `add active habit succeeds when active count is less than 6`() = runTest {
        // Given 5 active habits already in repository
        repeat(5) { i ->
            repository.insertHabit(createTestHabit(name = "Habit $i", isActive = true, reminderTimes = listOf("08:00")))
        }

        // When inserting a 6th active habit
        val activeHabit = createTestHabit(name = "Habit 6", isActive = true, reminderTimes = listOf("10:00"))
        val (result, savedHabit) = addHabitUseCase(activeHabit)

        // Then it succeeds as Activated
        assertEquals(ActivationResult.Activated, result)
        assertTrue(savedHabit.isActive)
        assertEquals(6, repository.getAllHabitsSync().size)
    }

    @Test
    fun `add active habit forces inactive when active count is already 6`() = runTest {
        // Given 6 active habits already in repository
        repeat(6) { i ->
            repository.insertHabit(createTestHabit(name = "Habit $i", isActive = true, reminderTimes = listOf("08:00")))
        }

        // When inserting a 7th active habit
        val activeHabit = createTestHabit(name = "Habit 7", isActive = true, reminderTimes = listOf("11:00"))
        val (result, savedHabit) = addHabitUseCase(activeHabit)

        // Then it is saved as Inactive, returning SavedAsInactive
        assertTrue(result is ActivationResult.SavedAsInactive)
        assertEquals(6, (result as ActivationResult.SavedAsInactive).currentActiveCount)
        assertFalse(savedHabit.isActive)
        assertEquals(7, repository.getAllHabitsSync().size)
    }
}
