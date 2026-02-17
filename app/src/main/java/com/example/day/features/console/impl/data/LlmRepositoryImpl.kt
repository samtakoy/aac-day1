package com.example.day.features.console.impl.data

import android.util.Log
import com.example.day.BuildConfig
import com.example.day.features.console.impl.data.remote.RemoteLlmApi
import com.example.day.features.console.impl.data.remote.mappers.ModelRequestMapper
import com.example.day.features.console.impl.data.remote.mappers.ModelResponseMapper
import com.example.day.features.console.impl.domain.LlmRepository
import com.example.day.features.console.impl.domain.model.ModelRequest
import com.example.day.features.console.impl.domain.model.ModelResult
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

internal class LlmRepositoryImpl @Inject constructor(
    private val api: RemoteLlmApi,
    private val requestMapper: ModelRequestMapper,
    private val responseMapper: ModelResponseMapper
) : LlmRepository {
    override suspend fun sendRequest(request: ModelRequest): ModelResult {
        return try {
            val result = api.sendRequest(
                request = requestMapper.toDto(request),
                apiKey = BuildConfig.LLM_API_KEY
            )
            responseMapper.toDomain(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("mytest", e.stackTraceToString())
            ModelResult.RuntimeError(e.stackTraceToString())
        }
    }
}