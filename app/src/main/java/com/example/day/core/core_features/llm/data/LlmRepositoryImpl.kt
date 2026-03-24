package com.example.day.core.core_features.llm.data

import android.util.Log
import com.example.day.BuildConfig
import com.example.day.core.app_settings.AppSettings
import com.example.day.core.core_features.llm.data.remote.LocalLlmApi
import com.example.day.core.core_features.llm.data.remote.RemoteLlmApi
import com.example.day.core.core_features.llm.data.remote.mappers.ModelRequestMapper
import com.example.day.core.core_features.llm.data.remote.mappers.ModelResponseMapper
import com.example.day.core.core_features.llm.data.remote.mappers.OpenAiModelRequestMapperImpl
import com.example.day.core.core_features.llm.data.remote.mappers.OpenAiModelResponseMapperImpl
import com.example.day.core.core_features.llm.domain.LlmRepository
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.ModelResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class LlmRepositoryImpl @Inject constructor(
    private val remoteApi: RemoteLlmApi,
    private val remoteRequestMapper: ModelRequestMapper,
    private val remoteResponseMapper: ModelResponseMapper,
    private val localApi: LocalLlmApi,
    private val localRequestMapper: OpenAiModelRequestMapperImpl,
    private val localResponseMapper: OpenAiModelResponseMapperImpl,
    private val appSettings: AppSettings
) : LlmRepository {
    override suspend fun sendRequest(request: ModelRequest): ModelResult {
        return try {
            if (request.isLocal) {
                val serverUrl = appSettings.localServerUrl.first()
                val dto = localRequestMapper.toDto(request)
                val response = localApi.sendRequest(dto, serverUrl)
                localResponseMapper.toDomain(response)
            } else {
                val result = remoteApi.sendRequest(
                    request = remoteRequestMapper.toDto(request),
                    apiKey = BuildConfig.LLM_API_KEY
                )
                remoteResponseMapper.toDomain(result)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("mytest", e.stackTraceToString())
            ModelResult.RuntimeError(e.stackTraceToString())
        }
    }
}
