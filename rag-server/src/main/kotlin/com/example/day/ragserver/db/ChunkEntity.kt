package com.example.day.ragserver.db

data class ChunkEntity(
    val id: Long = -1L,
    val content: String,
    val filePath: String,
    val fileName: String,
    val packageName: String = "",
    val declarationName: String? = null,
    val startLine: Int = 0,
    val strategy: String,
    val chunkOrder: Int,
    val indexedAt: String,
)

/**
 * Human-readable identifier for display in search results and logs.
 * Example: "com.example.day.features.chats · ChatRepository.kt:42 · searchByQuery"
 */
fun ChunkEntity.formatHeader(): String = buildString {
    if (packageName.isNotEmpty()) append("$packageName · ")
    append(fileName)
    if (startLine > 0) append(":$startLine")
    declarationName?.let { append(" · $it") }
}

data class SearchResult(
    val chunk: ChunkEntity,
    val score: Float,
)

data class ScoredChunk(
    val chunk: ChunkEntity,
    val embeddingScore: Double,
    val keywordScore: Double,
) {
    val finalScore: Double
        get() = 0.6 * embeddingScore + 0.4 * keywordScore
}

data class IndexStats(
    val totalChunks: Int,
    val structuralChunks: Int,
    val fixedChunks: Int,
    val indexedAt: String?,
    val isReady: Boolean,
)
