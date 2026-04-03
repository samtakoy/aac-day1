package com.example.day.core.core_features.agent.domain.workers.concrete

data class SubtaskDef(
    val text: String,
    val dependsOn: List<Int>?,  // null = не указано (legacy), [] = независимый, [1,2] = зависит от шагов 1 и 2
)

data class OrchestratorParsedResult(
    val taskDescription: String?,
    val subtasks: List<SubtaskDef>
)

object OrchestratorResultParser {

    private const val TAG_TASK_START = "[TASK_START]"
    private const val TAG_TASK_END = "[TASK_END]"
    private const val TAG_SUBTASKS_START = "[SUBTASKS_START]"
    private const val TAG_SUBTASKS_END = "[SUBTASKS_END]"
    private const val DEPENDS_ON_PREFIX = "depends_on:"

    fun parse(rawText: String): OrchestratorParsedResult? {
        if (!rawText.contains(TAG_TASK_START) && !rawText.contains(TAG_SUBTASKS_START)) {
            return null
        }

        val taskDescription = extractBetween(rawText, TAG_TASK_START, TAG_TASK_END)

        val subtasks = mutableListOf<SubtaskDef>()
        var searchFrom = 0
        while (true) {
            val start = rawText.indexOf(TAG_SUBTASKS_START, searchFrom)
            if (start == -1) break
            val contentStart = start + TAG_SUBTASKS_START.length
            val end = rawText.indexOf(TAG_SUBTASKS_END, contentStart)
            if (end == -1) break
            val raw = rawText.substring(contentStart, end).trim()
            subtasks.add(parseSubtask(raw))
            searchFrom = end + TAG_SUBTASKS_END.length
        }

        return OrchestratorParsedResult(taskDescription, subtasks)
    }

    private fun parseSubtask(raw: String): SubtaskDef {
        val firstLine = raw.lineSequence().firstOrNull()?.trim() ?: return SubtaskDef(raw, null)
        return if (firstLine.startsWith(DEPENDS_ON_PREFIX)) {
            val depsPart = firstLine.removePrefix(DEPENDS_ON_PREFIX).trim()
            val dependsOn = parseIntList(depsPart)
            val text = raw.substringAfter("\n").trim()
            SubtaskDef(text, dependsOn)
        } else {
            SubtaskDef(raw, null)
        }
    }

    // "[1, 2, 3]" или "[]" или "1,2" → List<Int>
    private fun parseIntList(raw: String): List<Int> {
        val cleaned = raw.trim().removePrefix("[").removeSuffix("]").trim()
        if (cleaned.isEmpty()) return emptyList()
        return cleaned.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    private fun extractBetween(text: String, startTag: String, endTag: String): String? {
        val start = text.indexOf(startTag)
        if (start == -1) return null
        val contentStart = start + startTag.length
        val end = text.indexOf(endTag, contentStart)
        if (end == -1) return null
        return text.substring(contentStart, end).trim()
    }
}
