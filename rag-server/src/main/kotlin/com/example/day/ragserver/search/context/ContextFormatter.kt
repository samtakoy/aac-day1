package com.example.day.ragserver.search.context

import com.example.day.ragserver.db.MethodInfo
import com.example.day.ragserver.db.SearchResult

object ContextFormatter {

    fun format(packed: PackedContext): String = buildString {
        appendLine("Found ${packed.groups.size} relevant class(es) | ~${packed.totalTokens * 4} chars")
        appendLine()

        // Сквозные счётчики: [КЛАСС N] для метаданных класса, [ИСТОЧНИК N] для чанков кода
        var sourceIndex = 1
        var classIndex = 1
        // Накапливаем строки для секции ## Источники в конце
        val sourcesTable = mutableListOf<String>()

        packed.groups.forEach { group ->
            appendLine("${"━".repeat(60)}")
            appendLine("[CLASS] ${group.className} [КЛАСС $classIndex]")
            appendLine("File: ${group.filePath}")
            appendLine("Score: ${"%.3f".format(group.topScore)}")

            group.responsibility?.let {
                appendLine("Responsibility: $it")
            }
            if (group.keyMethods.isNotEmpty()) {
                val methodsLine = group.keyMethods.joinToString(", ") { m ->
                    if (m.params != null) "${m.name}(${m.params})" else "${m.name}()"
                }
                appendLine("Key methods: $methodsLine")
            }
            appendLine()

            val classFileName = group.filePath.substringAfterLast("/")
            sourcesTable.add("[КЛАСС $classIndex] ${group.className} · $classFileName · ${group.filePath}")
            classIndex++

            group.chunks.forEach { chunk ->
                val label = chunk.declarationName ?: "line ${chunk.startLine}"
                appendLine("--- $label [ИСТОЧНИК $sourceIndex] (line ${chunk.startLine}) ---")
                appendLine(chunk.content.trim())
                appendLine()

                val fileName = chunk.filePath.substringAfterLast("/")
                val labelPart = if (label == fileName.removeSuffix(".kt")) "" else " · $label"
                sourcesTable.add("[ИСТОЧНИК $sourceIndex] $fileName$labelPart · line ${chunk.startLine} · ${chunk.filePath}")
                sourceIndex++
            }
        }

        appendLine("${"━".repeat(60)}")
        appendLine("## Источники")
        sourcesTable.forEach { appendLine(it) }
    }

    fun formatFlat(results: List<SearchResult>): String = buildString {
        val separator = "\n${"=".repeat(60)}\n"
        results.mapIndexed { i, r ->
            "[${i + 1}/${results.size}] score: ${"%.3f".format(r.score)}\n\n${r.chunk.content}"
        }.joinTo(this, separator)
    }
}
