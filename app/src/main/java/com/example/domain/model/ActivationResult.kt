package com.example.domain.model

sealed class ActivationResult {
    object Activated : ActivationResult()
    data class SavedAsInactive(val currentActiveCount: Int) : ActivationResult()
    object NotApplicable : ActivationResult()
}

const val MAX_ACTIVE_HABITS = 6
