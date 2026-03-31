package com.example.day.ragserver.indexing.chunking

import com.example.day.ragserver.db.ChunkEntity
import com.example.day.ragserver.indexing.ChunkingStrategy
import java.time.Instant

// Strategy D: Markdown — split by headers (#, ##, ###)
// Applies greedy-merging to prevent tiny chunks from short sections.
class MarkdownChunkingStrategy(
    val maxChunkSize: Int = DEFAULT_MAX_CHUNK_SIZE,
    val codePath: String? = null,
) : ChunkingStrategy {

    override val strategyName = "structural"

    private val HEADER_SPLIT_REGEX = Regex("""(?=\n#{1,3} )""")
    
    companion object {
        private const val MIN_MERGE_SIZE = 400
    }

    /**
     * Computes display name as relative path from codePath for .md files.
     * Falls back to original fileName if codePath is null or filePath doesn't start with codePath.
     */
    private fun computeDisplayName(filePath: String, originalFileName: String): String {
        if (codePath == null) return originalFileName
        val normalizedFilePath = filePath.replace('\\', '/')
        val normalizedCodePath = codePath.replace('\\', '/').trimEnd('/') + "/"
        return if (normalizedFilePath.startsWith(normalizedCodePath)) {
            normalizedFilePath.removePrefix(normalizedCodePath)
        } else {
            originalFileName
        }
    }

    /**
     * Greedy-merges adjacent sections to prevent tiny chunks.
     * Sections are merged until the accumulated size exceeds maxChunkSize.
     * This prevents degenerate embeddings from very short text segments.
     */
    private fun mergeSections(sections: List<String>): List<String> {
        if (sections.size <= 1) return sections

        val merged = mutableListOf<String>()
        var buffer = ""

        for (section in sections) {
            val candidate = if (buffer.isEmpty()) section else buffer + "\n\n" + section

            if (candidate.length <= maxChunkSize) {
                buffer = candidate
            } else {
                if (buffer.isNotEmpty()) {
                    merged.add(buffer)
                    buffer = ""
                }
                // If section itself exceeds maxChunkSize, emit as-is (will be sub-split later)
                if (section.length <= maxChunkSize) {
                    buffer = section
                } else {
                    merged.add(section)
                }
            }
        }

        if (buffer.isNotEmpty()) {
            merged.add(buffer)
        }

        return merged
    }

    override fun split(content: String, filePath: String, fileName: String): List<ChunkEntity> {
        if (content.isBlank()) return emptyList()

        val now = Instant.now().toString()
        val displayName = computeDisplayName(filePath, fileName)
        val header = "// File: $displayName\n"

        val sections = content.split(HEADER_SPLIT_REGEX).filter { it.isNotBlank() }

        // Greedy-merge small sections to prevent degenerate embeddings
        val mergedSections = mergeSections(sections)

        if (mergedSections.size <= 1) {
            // No headers or single section — fallback to fixed size
            return FixedSizeStrategy(maxChunkSize, maxChunkSize / 5)
                .split(content, filePath, displayName)
                .map { it.copy(strategy = strategyName) }
        }

        val chunks = mutableListOf<ChunkEntity>()
        var order = 0
        var currentLine = 1

        for (section in mergedSections) {
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
                        fileName = displayName,
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
                    .split(section.trim(), filePath, displayName)
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
