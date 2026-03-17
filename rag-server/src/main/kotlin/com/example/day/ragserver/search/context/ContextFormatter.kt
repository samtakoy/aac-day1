package com.example.day.ragserver.search.context

import com.example.day.ragserver.db.SearchResult

object ContextFormatter {

    fun format(packed: PackedContext): String = buildString {
        appendLine("Found ${packed.groups.size} relevant class(es) | ~${packed.totalTokens * 4} chars")
        appendLine()

        packed.groups.forEach { group ->
            appendLine("${"━".repeat(60)}")
            appendLine("[CLASS] ${group.className}")
            appendLine("File: ${group.filePath}")
            appendLine("Score: ${"%.3f".format(group.topScore)}")

            group.responsibility?.let {
                appendLine("Responsibility: $it")
            }

            val declarations = group.chunks.mapNotNull { it.declarationName }.distinct()
            if (declarations.isNotEmpty()) {
                appendLine("Declarations: ${declarations.joinToString(", ")}")
            }
            appendLine()

            group.chunks.forEach { chunk ->
                val label = chunk.declarationName ?: "line ${chunk.startLine}"
                appendLine("--- $label ---")
                appendLine(chunk.content.trim())
                appendLine()
            }
        }
    }

    fun formatFlat(results: List<SearchResult>): String = buildString {
        val separator = "\n${"=".repeat(60)}\n"
        results.mapIndexed { i, r ->
            "[${i + 1}/${results.size}] score: ${"%.3f".format(r.score)}\n\n${r.chunk.content}"
        }.joinTo(this, separator)
    }
}
