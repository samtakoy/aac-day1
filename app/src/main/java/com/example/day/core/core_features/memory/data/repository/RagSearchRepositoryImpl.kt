package com.example.day.core.core_features.memory.data.repository

import com.example.day.core.core_features.memory.domain.provider.rag.EvaluateResponse
import com.example.day.core.core_features.memory.domain.provider.rag.RagEvalItem
import com.example.day.core.core_features.memory.domain.provider.rag.RagSearchRepository
import com.example.day.core.core_features.memory.domain.provider.rag.CustomPipelineConfig
import com.example.day.core.core_features.memory.domain.provider.rag.RuntestResultItem
import com.example.day.core.core_features.memory.domain.provider.rag.RuntestSaveResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import javax.inject.Inject

class RagSearchRepositoryImpl @Inject constructor(
    private val httpClient: HttpClient
) : RagSearchRepository {

    @kotlinx.serialization.Serializable
    private data class SearchRequestDto(
        val query: String,
        val preset: String? = null,
        val taskState: String? = null,
        val history: String? = null,
    )

    override suspend fun search(
        query: String,
        serverUrl: String,
        taskStateJson: String?,
        shortHistory: String?,
        preset: String,
    ): Result<String> = runCatching {
        val url = "${serverUrl.trimEnd('/')}/search"
        android.util.Log.d("(rag)(ktor)", "HTTP POST $url query='${query.take(60)}'")
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(SearchRequestDto(
                query = query,
                preset = preset,
                taskState = taskStateJson?.takeIf { it.isNotBlank() && it != "{}" },
                history = shortHistory?.takeIf { it.isNotBlank() },
            ))
        }
        android.util.Log.d("(rag)(ktor)", "HTTP response: status=${response.status}")
        response.bodyAsText()
    }

    override suspend fun evaluate(
        questions: List<String>,
        presets: List<String>,
        serverUrl: String,
    ): Result<EvaluateResponse> = runCatching {
        httpClient.post("${serverUrl.trimEnd('/')}/evaluate") {
            contentType(ContentType.Application.Json)
            setBody(EvaluateRequestDto(questions = questions, presets = presets))
        }.body<EvaluateResponseDto>().toDomain()
    }

    override suspend fun evaluateCustom(
        customConfig: CustomPipelineConfig,
        questions: List<String>,
        serverUrl: String,
    ): Result<EvaluateResponse> = runCatching {
        httpClient.post("${serverUrl.trimEnd('/')}/evaluate") {
            contentType(ContentType.Application.Json)
            setBody(EvaluateRequestDto(
                questions = questions,
                presets = emptyList(),
                customConfig = CustomConfigDto(
                    retrievalTopK = customConfig.retrievalTopK,
                    threshold = customConfig.threshold,
                    rerankStrategy = customConfig.rerankStrategy,
                    finalTopK = customConfig.finalTopK,
                    enableQueryOptimize = customConfig.enableQueryOptimize,
                    retrievalStrategy = customConfig.retrievalStrategy,
                    chunkingStrategy = customConfig.chunkingStrategy,
                ),
            ))
        }.body<EvaluateResponseDto>().toDomain()
    }

    override suspend fun logLlmResponse(
        userMessage: String,
        assistantResponse: String,
        taskStateJson: String?,
        serverUrl: String,
    ): Result<Unit> = runCatching {
        httpClient.post("${serverUrl.trimEnd('/')}/session/log-response") {
            contentType(ContentType.Application.Json)
            setBody(LogLlmResponseDto(userMessage, assistantResponse, taskStateJson))
        }
        Unit
    }

    override suspend fun saveRuntestResults(
        preset: String,
        items: List<RuntestResultItem>,
        serverUrl: String,
        executionTimeMs: Long,
        isLocalLlm: Boolean,
    ): Result<RuntestSaveResponse> = runCatching {
        httpClient.post("${serverUrl.trimEnd('/')}/runtest/save") {
            contentType(ContentType.Application.Json)
            setBody(RuntestSaveRequestDto(
                preset = preset,
                items = items.map { RuntestItemDto(it.question, it.llmAnswer) },
                executionTimeMs = executionTimeMs,
                isLocalLlm = isLocalLlm,
            ))
        }.body<RuntestSaveResponseDto>().toDomain()
    }

    @Serializable
    private data class LogLlmResponseDto(
        val userMessage: String,
        val assistantResponse: String,
        val taskStateJson: String? = null,
    )

    @Serializable
    private data class RuntestItemDto(
        val question: String,
        val llmAnswer: String,
    )

    @Serializable
    private data class RuntestSaveRequestDto(
        val preset: String,
        val items: List<RuntestItemDto>,
        val executionTimeMs: Long = 0,
        val isLocalLlm: Boolean = false,
    )

    @Serializable
    private data class RuntestSaveResponseDto(
        val savedReport: String,
    ) {
        fun toDomain() = RuntestSaveResponse(savedReport = savedReport)
    }

    @Serializable
    private data class CustomConfigDto(
        val retrievalTopK: Int? = null,
        val threshold: Double? = null,
        val rerankStrategy: String? = null,
        val finalTopK: Int? = null,
        val enableQueryOptimize: Boolean? = null,
        val retrievalStrategy: String? = null,
        val chunkingStrategy: String? = null,
    )

    @Serializable
    private data class EvaluateRequestDto(
        val questions: List<String>,
        val presets: List<String>,
        val customConfig: CustomConfigDto? = null,
    )

    @Serializable
    private data class EvalItemDto(
        val question: String,
        val optimizedQuery: String? = null,
        val ragContext: String,
    )

    @Serializable
    private data class EvaluateResponseDto(
        val savedReports: List<String>,
        val summary: String,
        val items: Map<String, List<EvalItemDto>> = emptyMap(),
    ) {
        fun toDomain() = EvaluateResponse(
            savedReports = savedReports,
            summary = summary,
            items = items.mapValues { (_, list) ->
                list.map { RagEvalItem(it.question, it.optimizedQuery, it.ragContext) }
            },
        )
    }
}
