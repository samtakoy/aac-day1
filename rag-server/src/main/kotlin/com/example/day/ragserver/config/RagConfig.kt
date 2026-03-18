package com.example.day.ragserver.config

data class RagConfig(
    val codePath: String,
    val dbPath: String,
    val embeddingProvider: String,
    val ollamaBaseUrl: String,
    val embeddingModel: String,
    val openRouterApiKey: String,
    val serverPort: Int,
    val forceReindex: Boolean,
    val searchTopK: Int,
    val llmModel: String,
    val extractMetadata: Boolean,
    // Query optimization (rewrite + translate) перед поиском.
    // Включать если запросы приходят на русском/другом языке — улучшает
    // как keyword Stage 1, так и embedding similarity с английским кодом.
    // Также добавляет технические ключевые слова для любых запросов.
    val translateQueries: Boolean,
    // Модель для query optimization. По умолчанию та же что для метаданных.
    val translateLlmModel: String,
    // Модель для LLM reranker. По умолчанию та же что llmModel.
    // Можно задать быструю модель отдельно: RERANKER_LLM_MODEL=qwen2.5:3b
    val rerankerLlmModel: String,
) {
    companion object {
        fun from(): RagConfig {
            val codePath = System.getenv("CODE_PATH")
                ?: error("CODE_PATH environment variable is required")
            val llmModel = System.getenv("LLM_MODEL") ?: "qwen2.5-coder:7b-instruct"
            return RagConfig(
                codePath = codePath,
                dbPath = System.getenv("DB_PATH") ?: "./rag_index.db",
                embeddingProvider = System.getenv("EMBEDDING_PROVIDER") ?: "ollama",
                ollamaBaseUrl = System.getenv("OLLAMA_BASE_URL") ?: "http://localhost:11434",
                embeddingModel = System.getenv("EMBEDDING_MODEL") ?: "nomic-embed-text",
                openRouterApiKey = System.getenv("OPENROUTER_API_KEY") ?: "",
                serverPort = System.getenv("RAG_SERVER_PORT")?.toIntOrNull() ?: 3001,
                forceReindex = System.getenv("FORCE_REINDEX")?.toBooleanStrictOrNull() ?: false,
                searchTopK = System.getenv("SEARCH_TOP_K")?.toIntOrNull() ?: 5,
                llmModel = llmModel,
                extractMetadata = System.getenv("EXTRACT_METADATA")?.toBooleanStrictOrNull() ?: false,
                translateQueries = System.getenv("TRANSLATE_QUERIES")?.toBooleanStrictOrNull() ?: false,
                translateLlmModel = System.getenv("TRANSLATE_LLM_MODEL") ?: llmModel,
                rerankerLlmModel = System.getenv("RERANKER_LLM_MODEL") ?: llmModel,
            )
        }
    }
}
