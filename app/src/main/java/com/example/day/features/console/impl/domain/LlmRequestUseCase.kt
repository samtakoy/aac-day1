package com.example.day.features.console.impl.domain

import javax.inject.Inject

internal class LlmRequestUseCase @Inject constructor(
    private val repository: LlmRepository
) {
    fun exec(): Result<String> {
        repository.test()
        return Result.success("ok")
    }
}