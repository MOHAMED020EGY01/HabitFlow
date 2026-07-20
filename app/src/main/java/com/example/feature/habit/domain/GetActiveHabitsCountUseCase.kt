package com.example.feature.habit.domain

import com.example.core.repository.HabitRepository
import kotlinx.coroutines.flow.Flow

class GetActiveHabitsCountUseCase(
    private val repository: HabitRepository
) {
    operator fun invoke(): Flow<Int> = repository.observeActiveHabitsCount()
}
