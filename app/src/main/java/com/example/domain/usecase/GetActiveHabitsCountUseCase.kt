package com.example.domain.usecase

import com.example.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow

class GetActiveHabitsCountUseCase(
    private val repository: HabitRepository
) {
    operator fun invoke(): Flow<Int> = repository.observeActiveHabitsCount()
}
