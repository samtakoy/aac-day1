package com.example.day.ragserver.indexing.chunking

import com.example.day.ragserver.db.ChunkEntity
import com.example.day.ragserver.indexing.ChunkingStrategy
import java.time.Instant

// Strategy D: Markdown — split by headers (#, ##, ###)
class MarkdownChunkingStrategy(
    val maxChunkSize: Int = DEFAULT_MAX_CHUNK_SIZE,
) : ChunkingStrategy {

    override val strategyName = "structural"

    private val HEADER_SPLIT_REGEX = Regex("""(?=\n#{1,3} )""")

    override fun split(content: String, filePath: String, fileName: String): List<ChunkEntity> {
        if (content.isBlank()) return emptyList()

        val now = Instant.now().toString()
        val header = "// File: $fileName\n"

        val sections = content.split(HEADER_SPLIT_REGEX).filter { it.isNotBlank() }

        if (sections.size <= 1) {
            // Нет заголовков — fallback на fixed
            return FixedSizeStrategy(maxChunkSize, maxChunkSize / 5)
                .split(content, filePath, fileName)
                .map { it.copy(strategy = strategyName) }
        }

        val chunks = mutableListOf<ChunkEntity>()
        var order = 0
        var currentLine = 1

        for (section in sections) {
            val declarationName = section.lines()
                .firstOrNull { it.startsWith("#") }
                ?.trimStart('#', ' ')
                ?.trim()

            val blockWithHeader = header + section.trim()
            val startLine = currentLine

            if (blockWithHeader.length <= maxChunkSize) {
                chunks.add(
                    ChunkEntity(
                        content = blockWithHeader,
                        filePath = filePath,
                        fileName = fileName,
                        packageName = "",
                        declarationName = declarationName,
                        startLine = startLine,
                        strategy = strategyName,
                        chunkOrder = order++,
                        indexedAt = now,
                    )
                )
            } else {
                val subChunks = FixedSizeStrategy(maxChunkSize, maxChunkSize / 5)
                    .split(section.trim(), filePath, fileName)
                subChunks.forEach { sub ->
                    chunks.add(
                        sub.copy(
                            strategy = strategyName,
                            declarationName = declarationName,
                            startLine = startLine + (sub.startLine - 1),
                            chunkOrder = order++,
                            indexedAt = now,
                        )
                    )
                }
            }

            currentLine += section.count { it == '\n' }
        }

        return chunks
    }
}
