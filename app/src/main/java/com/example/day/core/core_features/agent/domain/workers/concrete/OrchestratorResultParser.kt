package com.example.day.core.core_features.agent.domain.workers.concrete

data class OrchestratorParsedResult(
    val taskDescription: String?,
    val subtasks: List<String>
)

object OrchestratorResultParser {

    private const val TAG_TASK_START = "[TASK_START]"
    private const val TAG_TASK_END = "[TASK_END]"
    private const val TAG_SUBTASKS_START = "[SUBTASKS_START]"
    private const val TAG_SUBTASKS_END = "[SUBTASKS_END]"

    fun parse(rawText: String): OrchestratorParsedResult? {
        if (!rawText.contains(TAG_TASK_START) && !rawText.contains(TAG_SUBTASKS_START)) {
            return null
        }

        val taskDescription = extractBetween(rawText, TAG_TASK_START, TAG_TASK_END)

        val subtasks = mutableListOf<String>()
        var searchFrom = 0
        while (true) {
            val start = rawText.indexOf(TAG_SUBTASKS_START, searchFrom)
            if (start == -1) break
            val contentStart = start + TAG_SUBTASKS_START.length
            val end = rawText.indexOf(TAG_SUBTASKS_END, contentStart)
            if (end == -1) break
            subtasks.add(rawText.substring(contentStart, end).trim())
            searchFrom = end + TAG_SUBTASKS_END.length
        }

        return OrchestratorParsedResult(taskDescription, subtasks)
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
