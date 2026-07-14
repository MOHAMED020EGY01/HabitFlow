package com.example.domain.usecase

import com.example.domain.model.Habit
import com.example.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow

class GetAllHabitsUseCase(private val repository: HabitRepository) {
    operator fun invoke(): Flow<List<Habit>> = repository.getAllHabits()
}
