package com.example.day.core.core_features.agent.domain.workers.task.prompt

import kotlinx.serialization.json.Json

/**
 * Sanitizes data before injecting into prompts.
 * Prevents prompt injection attacks and JSON corruption.
 */
object PromptSanitizer {

    private val json = Json { encodeDefaults = false }

    /**
     * Escape special characters to prevent breaking JSON structure
     * or injecting malicious instructions.
     *
     * Uses kotlinx.serialization.json.Json.encodeToString() for proper JSON escaping.
     *
     * @param value Raw value from LTM or user input
     * @return Sanitized value safe for prompt injection
     */
    fun sanitize(value: String): String {
        // Json.encodeToString() adds surrounding quotes, so we remove them
        return json.encodeToString(value).drop(1).dropLast(1)
    }

    /**
     * Sanitize for use in system prompts where we don't want
     * the LLM to interpret special markers.
     *
     * @param value Raw value
     * @return Sanitized value safe for system prompt
     */
    fun sanitizeForSystemPrompt(value: String): String {
        return sanitize(value)
            // Break protocol markers and code blocks with explicit placeholders
            .replace("===", "[EQ][EQ][EQ]")
            .replace("```", "[BT][BT][BT]")
    }

    /**
     * Truncate value to maximum length to prevent token overflow.
     *
     * @param value Raw value
     * @param maxLength Maximum allowed length
     * @param suffix Suffix to add if truncated (default: "...")
     * @return Truncated value
     */
    fun truncate(value: String, maxLength: Int = 2000, suffix: String = "..."): String {
        return if (value.length > maxLength) {
            value.take(maxLength - suffix.length) + suffix
        } else {
            value
        }
    }

    /**
     * Full sanitization pipeline: sanitize + truncate.
     *
     * @param value Raw value
     * @param maxLength Maximum allowed length
     * @return Fully sanitized value
     */
    fun sanitizeAndTruncate(value: String, maxLength: Int = 2000): String {
        return truncate(sanitize(value), maxLength)
    }

    /**
     * Sanitize a map of key-value pairs.
     *
     * @param data Map of raw values
     * @return Map of sanitized values
     */
    fun sanitizeMap(data: Map<String, String>): Map<String, String> {
        return data.mapValues { (_, value) ->
            sanitize(value)
        }
    }
}