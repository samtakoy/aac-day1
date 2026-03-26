package com.example.day.ragserver.indexing.chunking

import com.example.day.ragserver.db.ChunkEntity
import com.example.day.ragserver.indexing.ChunkingStrategy
import com.example.day.ragserver.indexing.chunking.ast.AstChunkingStrategy

internal const val DEFAULT_MAX_CHUNK_SIZE = 2000

// Strategy E: routes by file extension + useAst flag.
// Implements ChunkingStrategy so IndexingService needs no changes.
class LanguageAwareChunker(
    private val useAst: Boolean,
    private val maxChunkSize: Int = DEFAULT_MAX_CHUNK_SIZE,
) : ChunkingStrategy {

    override val strategyName = "structural"

    private val astStrategy by lazy { AstChunkingStrategy.create(maxChunkSize) }
    private val markdownStrategy = MarkdownChunkingStrategy(maxChunkSize)
    private val legacyStrategy = StructuralStrategy(maxChunkSize)
    private val fixedFallback = FixedSizeStrategy(maxChunkSize, maxChunkSize / 5)

    override fun split(content: String, filePath: String, fileName: String): List<ChunkEntity> {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when {
            ext in setOf("kt", "kts") && useAst -> astStrategy.split(content, filePath, fileName)
            ext in setOf("kt", "kts") -> legacyStrategy.split(content, filePath, fileName)
            ext == "md" -> markdownStrategy.split(content, filePath, fileName)
            else -> fixedFallback.split(content, filePath, fileName)
                .map { it.copy(strategy = strategyName) }
        }
    }
}
