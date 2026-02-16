package com.example.day.features.console.impl.data

import com.example.day.features.console.impl.data.remote.RemoteLlmApi
import com.example.day.features.console.impl.domain.LlmRepository
import javax.inject.Inject

internal class LlmRepositoryImpl @Inject constructor(
    private val api: RemoteLlmApi
) : LlmRepository {
    override suspend fun sendRequest(prompt: String): Result<String> {
        return api.sendRequest(prompt)
    }
}