package com.example.day.core.core_features.memory.domain.provider.rag

data class CustomPipelineConfig(
    val retrievalTopK: Int? = null,
    val threshold: Double? = null,
    val rerankStrategy: String? = null,
    val finalTopK: Int? = null,
    val enableQueryOptimize: Boolean? = null,
    val retrievalStrategy: String? = null,
    val chunkingStrategy: String? = null,
)

data class RuntestResultItem(
    val question: String,
    val llmAnswer: String,
)

data class RuntestSaveResponse(
    val savedReport: String,
)

interface RagSearchRepository {
    /**
     * Search codebase via rag-server REST endpoint.
     * @param query user query text
     * @param serverUrl base URL, e.g. "http://10.0.2.2:3001"
     * @param taskStateJson JSON строка TaskState (опционально, для QueryOptimizer)
     * @param shortHistory сжатая история диалога (опционально, для QueryOptimizer)
     * @param preset pipeline preset, по умолчанию "reranked_llm"
     * @return formatted search results or failure
     */
    suspend fun search(
        query: String,
        serverUrl: String,
        taskStateJson: String? = null,
        shortHistory: String? = null,
        preset: String = "reranked_llm",
    ): Result<String>

    /**
     * Run evaluation: send questions through multiple pipeline presets.
     * Server saves MD reports to ./reports/ and returns summary.
     * @param questions list of test questions
     * @param presets preset names, e.g. ["baseline", "filtered"] or ["all"]
     * @param serverUrl base URL
     */
    suspend fun evaluate(
        questions: List<String>,
        presets: List<String>,
        serverUrl: String,
    ): Result<EvaluateResponse>

    suspend fun evaluateCustom(
        customConfig: CustomPipelineConfig,
        questions: List<String>,
        serverUrl: String,
    ): Result<EvaluateResponse>

    suspend fun saveRuntestResults(
        preset: String,
        items: List<RuntestResultItem>,
        serverUrl: String,
        executionTimeMs: Long = 0,
        isLocalLlm: Boolean = false,
    ): Result<RuntestSaveResponse>

    /**
     * Отправляет ответ LLM на rag-server для логирования в session-файл.
     * Fire-and-forget: ошибки не критичны, только логируются.
     */
    suspend fun logLlmResponse(
        userMessage: String,
        assistantResponse: String,
        taskStateJson: String?,
        serverUrl: String,
    ): Result<Unit>
}

data class RagEvalItem(
    val question: String,
    val optimizedQuery: String?,
    val ragContext: String,
)

data class EvaluateResponse(
    val savedReports: List<String>,
    val summary: String,
    val items: Map<String, List<RagEvalItem>>,
)
