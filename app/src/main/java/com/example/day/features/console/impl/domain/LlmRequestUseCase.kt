package com.example.day.features.console.impl.domain

import javax.inject.Inject

internal class LlmRequestUseCase @Inject constructor(
    private val repository: LlmRepository
) {
    suspend fun exec(promptText: String): Result<String> {
        return repository.sendRequest(promptText)
    }
}