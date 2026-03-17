package com.example.day.ragserver.embedding

import com.example.day.ragserver.config.RagConfig
import io.ktor.client.HttpClient

fun interface EmbeddingProvider {
    suspend fun embed(text: String): FloatArray
}

fun createEmbeddingProvider(config: RagConfig, httpClient: HttpClient): EmbeddingProvider {
    return when (config.embeddingProvider) {
        "ollama" -> OllamaEmbeddingProvider(config.ollamaBaseUrl, config.embeddingModel, httpClient)
        "openrouter" -> {
            require(config.openRouterApiKey.isNotBlank()) {
                "OPENROUTER_API_KEY is required when EMBEDDING_PROVIDER=openrouter"
            }
            OpenRouterEmbeddingProvider(config.openRouterApiKey, config.embeddingModel, httpClient)
        }
        else -> error("Unknown EMBEDDING_PROVIDER: '${config.embeddingProvider}'. Use 'ollama' or 'openrouter'")
    }
}
