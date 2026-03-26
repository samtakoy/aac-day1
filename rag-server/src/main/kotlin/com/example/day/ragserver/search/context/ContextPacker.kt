package com.example.day.ragserver.search.context

import com.example.day.ragserver.db.ChunkEntity
import com.example.day.ragserver.db.CodeDatabase
import com.example.day.ragserver.db.MethodInfo
import com.example.day.ragserver.db.SearchResult

class ContextPacker(
    private val db: CodeDatabase? = null,
    private val tokenLimit: Int = 6000,
) {

    fun pack(results: List<SearchResult>): PackedContext {
        // Группировка по filePath — уникальный ключ файла.
        // Ранее было groupBy { fileName.removeSuffix(".kt") }, что приводило к слиянию
        // чанков из разных файлов с одинаковым именем (например, Utils.kt в разных пакетах).
        val byFile = results.groupBy { it.chunk.filePath }

        val sortedGroups = byFile.entries
            .sortedByDescending { (_, groupResults) -> groupResults.maxOf { it.score } }

        val groups = mutableListOf<ClassGroup>()
        var usedTokens = 0

        for ((filePath, fileResults) in sortedGroups) {
            if (usedTokens >= tokenLimit) break

            val uniqueChunks = fileResults
                .map { it.chunk }
                .distinctBy { it.content.trim().hashCode() }
                .sortedBy { it.startLine }

            val groupTokens = uniqueChunks.sumOf { estimateTokens(it.content) }

            // Пропускаем группу если лимит превышен (кроме первой — её берём всегда)
            if (usedTokens + groupTokens > tokenLimit && groups.isNotEmpty()) continue

            val className = uniqueChunks.first().fileName.removeSuffix(".kt")
            val metadata = db?.getClassMetadataByFilePath(filePath)

            groups.add(
                ClassGroup(
                    className = className,
                    filePath = filePath,
                    chunks = uniqueChunks,
                    topScore = fileResults.maxOf { it.score },
                    responsibility = metadata?.responsibility?.takeIf { it.isNotBlank() },
                    keyMethods = metadata?.keyMethods ?: emptyList(),
                )
            )
            usedTokens += groupTokens
        }

        return PackedContext(groups = groups, totalTokens = usedTokens)
    }

    private fun estimateTokens(text: String): Int = text.length / 4
}

data class PackedContext(
    val groups: List<ClassGroup>,
    val totalTokens: Int,
)

data class ClassGroup(
    val className: String,
    val filePath: String,
    val chunks: List<ChunkEntity>,
    val topScore: Float,
    val responsibility: String? = null,
    val keyMethods: List<MethodInfo> = emptyList(),
)
