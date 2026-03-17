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
) {
    companion object {
        fun from(): RagConfig {
            val codePath = System.getenv("CODE_PATH")
                ?: error("CODE_PATH environment variable is required")
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
            )
        }
    }
}
