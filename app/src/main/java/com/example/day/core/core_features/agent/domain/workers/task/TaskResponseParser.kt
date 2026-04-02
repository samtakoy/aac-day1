package com.example.day.core.core_features.agent.domain.workers.task

import com.example.day.core.core_features.agent.domain.model.TaskLlmResponse
import com.example.day.core.core_features.agent.domain.workers.task.util.sanitizeJsonStringValues
import kotlinx.serialization.json.Json

/**
 * Parser for LLM JSON responses in task workflow.
 * Handles markdown code blocks and extracts JSON from various formats.
 */
object TaskResponseParser {

    private val json = Json {
        coerceInputValues = true // Принудительно заменяет null на дефолтное значение из конструктора
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parse raw LLM response into TaskLlmResponse.
     * Handles markdown code blocks and plain JSON.
     *
     * @param rawResponse Raw response from LLM
     * @return Parsed TaskLlmResponse or null if parsing failed
     */
    fun parse(rawResponse: String): TaskLlmResponse? {
        return try {
            val jsonContent = extractJson(rawResponse)
            val sanitized = sanitizeJsonStringValues(jsonContent)
            json.decodeFromString(TaskLlmResponse.serializer(), sanitized)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extract JSON from response that may be wrapped in markdown.
     * NOTE: code-block search is only applied when the response is NOT already raw JSON,
     * to avoid matching ``` blocks embedded inside a JSON string value.
     */
    internal fun extractJson(response: String): String {
        val trimmed = response.trim()

        // Already a JSON object — return as-is (code blocks inside string values must not be touched)
        if (trimmed.startsWith('{')) return trimmed

        // Wrapped in a markdown code block — extract the inner content
        val codeBlockRegex = """```(?:json)?\s*([\s\S]*?)```""".toRegex()
        val match = codeBlockRegex.find(trimmed)
        if (match != null) return match.groupValues[1].trim()

        // Last resort: find first { … last }
        val jsonStart = trimmed.indexOf('{')
        val jsonEnd = trimmed.lastIndexOf('}')
        return if (jsonStart >= 0 && jsonEnd > jsonStart) {
            trimmed.substring(jsonStart, jsonEnd + 1)
        } else {
            trimmed
        }
    }

}
