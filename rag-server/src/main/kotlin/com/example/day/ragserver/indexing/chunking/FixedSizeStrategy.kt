package com.example.day.ragserver.indexing.chunking

import com.example.day.ragserver.db.ChunkEntity
import com.example.day.ragserver.indexing.ChunkingStrategy
import com.example.day.ragserver.indexing.CodeMetadata
import java.time.Instant

class FixedSizeStrategy(
    val chunkSize: Int = 1000,
    val overlap: Int = 200,
) : ChunkingStrategy {

    override val strategyName = "fixed"

    override fun split(content: String, filePath: String, fileName: String): List<ChunkEntity> {
        if (content.isBlank()) return emptyList()

        val now = Instant.now().toString()
        val header = "// File: $fileName\n"
        val packageName = CodeMetadata.extractPackage(content)

        if (content.length <= chunkSize) {
            return listOf(
                ChunkEntity(
                    content = header + content,
                    filePath = filePath, fileName = fileName,
                    packageName = packageName, startLine = 1,
                    strategy = strategyName, chunkOrder = 0, indexedAt = now,
                )
            )
        }

        val chunks = mutableListOf<ChunkEntity>()
        val step = chunkSize - overlap
        var order = 0
        var charOffset = 0

        while (charOffset < content.length) {
            val end = minOf(charOffset + chunkSize, content.length)
            val startLine = 1 + content.substring(0, charOffset).count { it == '\n' }
            chunks.add(
                ChunkEntity(
                    content = header + content.substring(charOffset, end),
                    filePath = filePath, fileName = fileName,
                    packageName = packageName, startLine = startLine,
                    strategy = strategyName, chunkOrder = order++, indexedAt = now,
                )
            )
            if (end == content.length) break
            charOffset += step
        }

        return chunks
    }
}
