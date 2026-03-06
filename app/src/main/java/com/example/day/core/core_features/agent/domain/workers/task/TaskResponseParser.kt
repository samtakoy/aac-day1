package com.example.day.core.core_features.agent.domain.workers.task

import com.example.day.core.core_features.agent.domain.model.TaskLlmResponse
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
            // Try to extract JSON from markdown code block
            val jsonContent = extractJson(rawResponse)
            json.decodeFromString(TaskLlmResponse.serializer(), jsonContent)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extract JSON from response that may be wrapped in markdown.
     */
    private fun extractJson(response: String): String {
        // Try to find JSON in markdown code block
        val codeBlockRegex = """```(?:json)?\s*([\s\S]*?)```""".toRegex()
        val match = codeBlockRegex.find(response)

        return if (match != null) {
            match.groupValues[1].trim()
        } else {
            // Try to find JSON object directly
            val jsonStart = response.indexOf('{')
            val jsonEnd = response.lastIndexOf('}')

            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                response.substring(jsonStart, jsonEnd + 1)
            } else {
                response.trim()
            }
        }
    }
}
