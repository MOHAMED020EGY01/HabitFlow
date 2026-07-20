package com.example.feature.habit.domain

import com.example.core.model.domain.Habit
import com.example.core.repository.HabitRepository
import kotlinx.coroutines.flow.Flow

class GetAllHabitsUseCase(private val repository: HabitRepository) {
    operator fun invoke(): Flow<List<Habit>> = repository.getAllHabits()
}
