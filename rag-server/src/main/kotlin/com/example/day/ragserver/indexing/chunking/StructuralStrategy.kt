package com.example.day.ragserver.indexing.chunking

import com.example.day.ragserver.db.ChunkEntity
import com.example.day.ragserver.indexing.ChunkingStrategy
import com.example.day.ragserver.indexing.CodeMetadata
import java.time.Instant

// Strategy B: Structural — split by Kotlin top-level declarations via regex,
// keeping KDoc comments and annotations attached to their declaration.
class StructuralStrategy(
    val maxChunkSize: Int = 2000,
) : ChunkingStrategy {

    override val strategyName = "structural"

    private val SPLIT_REGEX = Regex(
        """(?=\n(?:fun |class |interface |object |data class |sealed |enum |abstract |companion |typealias ))"""
    )

    override fun split(content: String, filePath: String, fileName: String): List<ChunkEntity> {
        if (content.isBlank()) return emptyList()

        val now = Instant.now().toString()
        val header = "// File: $fileName\n"
        val packageName = CodeMetadata.extractPackage(content)

        val rawBlocks = content.split(SPLIT_REGEX).filter { it.isNotBlank() }

        // Attach KDoc comments and annotations to the declaration that follows them
        val blocks = attachPreamblesForward(rawBlocks)

        // Line offsets computed from raw blocks (before preamble movement) give approximate
        // but stable file positions — sufficient for navigation.
        val lineOffsets = computeLineOffsets(rawBlocks)

        val chunks = mutableListOf<ChunkEntity>()
        var order = 0

        for ((index, block) in blocks.withIndex()) {
            val blockWithHeader = header + block.trim()
            val startLine = lineOffsets.getOrElse(index) { 1 }
            val declarationName = CodeMetadata.extractDeclarationName(block)

            if (blockWithHeader.length <= maxChunkSize) {
                chunks.add(
                    ChunkEntity(
                        content = blockWithHeader,
                        filePath = filePath, fileName = fileName,
                        packageName = packageName, declarationName = declarationName,
                        startLine = startLine,
                        strategy = strategyName, chunkOrder = order++, indexedAt = now,
                    )
                )
            } else {
                // Block too large — sub-split with FixedSize, preserving structural metadata
                val subChunks = FixedSizeStrategy(maxChunkSize, maxChunkSize / 5)
                    .split(block.trim(), filePath, fileName)
                subChunks.forEach { sub ->
                    chunks.add(
                        sub.copy(
                            strategy = strategyName,
                            packageName = packageName,
                            declarationName = declarationName,
                            startLine = startLine + (sub.startLine - 1),
                            chunkOrder = order++,
                            indexedAt = now,
                        )
                    )
                }
            }
        }

        return chunks
    }

    /**
     * Moves trailing KDoc comments and annotations from the end of each block
     * to the beginning of the next block, keeping declarations paired with their docs.
     */
    private fun attachPreamblesForward(blocks: List<String>): List<String> {
        if (blocks.size <= 1) return blocks

        val result = ArrayList<String>(blocks.size)
        var pendingPreamble = ""

        for (block in blocks) {
            val full = pendingPreamble + block
            val (body, preamble) = extractTrailingPreamble(full)
            result.add(body)
            pendingPreamble = preamble
        }

        // The last block has nowhere to forward its preamble — keep it in place
        if (pendingPreamble.isNotBlank() && result.isNotEmpty()) {
            result[result.lastIndex] = result.last() + pendingPreamble
        }

        return result.filter { it.isNotBlank() }
    }

    /**
     * Splits a block into (mainBody, trailingPreamble).
     * Trailing preamble = annotation lines and comment blocks that appear after
     * the last "real" code line. These belong to the next declaration.
     */
    private fun extractTrailingPreamble(block: String): Pair<String, String> {
        val lines = block.lines()

        var lastNonBlank = lines.lastIndex
        while (lastNonBlank >= 0 && lines[lastNonBlank].trim().isEmpty()) lastNonBlank--

        var preambleStart = lastNonBlank + 1
        var i = lastNonBlank
        while (i >= 0) {
            val trimmed = lines[i].trim()
            if (trimmed.startsWith("@") ||
                trimmed.startsWith("//") ||
                trimmed.startsWith("/*") ||
                trimmed.startsWith("*")
            ) {
                preambleStart = i
                i--
            } else {
                break
            }
        }

        if (preambleStart > lastNonBlank) return block to ""

        val main = lines.subList(0, preambleStart).joinToString("\n")
        val preamble = "\n" + lines.subList(preambleStart, lines.size).joinToString("\n")
        return main to preamble
    }

    private fun computeLineOffsets(rawBlocks: List<String>): List<Int> {
        val offsets = ArrayList<Int>(rawBlocks.size)
        var line = 1
        for (block in rawBlocks) {
            offsets.add(line)
            line += block.count { it == '\n' }
        }
        return offsets
    }
}
