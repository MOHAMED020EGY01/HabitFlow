package com.example.presentation.screens.add

import androidx.test.core.app.ApplicationProvider
import com.example.HabitApplication
import com.example.domain.repository.FakeHabitRepository
import com.example.domain.usecase.AddHabitUseCase
import com.example.domain.usecase.GetActiveHabitsCountUseCase
import com.example.domain.usecase.UpdateHabitUseCase
import com.example.domain.usecase.ValidateReminderTimeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AddHabitViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: HabitApplication
    private lateinit var fakeRepository: FakeHabitRepository
    private lateinit var viewModel: AddHabitViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()

        // Inject fakes into HabitApplication
        fakeRepository = FakeHabitRepository()
        app.repository = fakeRepository
        app.addHabitUseCase = AddHabitUseCase(fakeRepository, app)
        app.updateHabitUseCase = UpdateHabitUseCase(fakeRepository, app)
        app.validateReminderTimeUseCase = ValidateReminderTimeUseCase(fakeRepository)
        app.getActiveHabitsCountUseCase = GetActiveHabitsCountUseCase(fakeRepository)

        viewModel = AddHabitViewModel(app)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        assertEquals("", state.name)
        assertEquals("", state.description)
        assertEquals(30, state.durationDays)
        assertEquals(listOf("09:00"), state.reminderTimes)
        assertNull(state.nameError)
        assertNull(state.durationError)
        assertNull(state.reminderTimesError)
    }

    @Test
    fun `saveHabit rejects empty name`() {
        // Given name is empty
        viewModel.onNameChange("")
        
        // When saving
        viewModel.saveHabit()
        
        // Then name error is set
        assertEquals("Habit name cannot be empty", viewModel.uiState.value.nameError)
    }

    @Test
    fun `saveHabit rejects invalid duration`() {
        // Given duration is 0
        viewModel.onNameChange("Valid Name")
        viewModel.onDurationChange(0)
        
        // When saving
        viewModel.saveHabit()
        
        // Then duration error is set
        assertEquals("Duration must be greater than 0", viewModel.uiState.value.durationError)
    }

    @Test
    fun `saveHabit rejects empty reminder times`() {
        // Given reminder list is empty
        viewModel.onNameChange("Valid Name")
        viewModel.onDurationChange(30)
        viewModel.removeReminderTime("09:00") // List is now empty
        
        // When saving
        viewModel.saveHabit()
        
        // Then reminder error is set
        assertEquals("Add at least one reminder time", viewModel.uiState.value.reminderTimesError)
    }

    @Test
    fun `saveHabit with valid input saves successfully`() = runTest {
        // Given valid inputs
        viewModel.onNameChange("Exercise")
        viewModel.onDescriptionChange("30 mins cardio")
        viewModel.onDurationChange(21)
        
        // When saving
        viewModel.saveHabit()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then saveSuccess is true and habit is inserted
        val state = viewModel.uiState.value
        assertTrue(state.saveSuccess)
        
        val savedHabits = fakeRepository.getAllHabitsSync()
        assertEquals(1, savedHabits.size)
        assertEquals("Exercise", savedHabits[0].name)
        assertEquals("30 mins cardio", savedHabits[0].description)
        assertEquals(21, savedHabits[0].durationDays)
    }
}
