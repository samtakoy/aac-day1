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
     * @return formatted search results or failure
     */
    suspend fun search(query: String, serverUrl: String): Result<String>

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
    ): Result<RuntestSaveResponse>
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
