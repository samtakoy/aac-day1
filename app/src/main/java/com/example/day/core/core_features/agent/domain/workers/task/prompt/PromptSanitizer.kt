package com.example.day.core.core_features.agent.domain.workers.task.prompt

/**
 * Sanitizes data before injecting into prompts.
 * Prevents prompt injection attacks and JSON corruption.
 */
object PromptSanitizer {

    /**
     * Escape special characters to prevent breaking JSON structure
     * or injecting malicious instructions.
     *
     * @param value Raw value from LTM or user input
     * @return Sanitized value safe for prompt injection
     */
    fun sanitize(value: String): String {
        return value
            // Escape JSON special characters first
            .replace("\\", "\\\\")  // Backslash must be first
            .replace("\"", "\\\"")  // Double quotes
            .replace("\n", "\\n")   // Newlines
            .replace("\r", "\\r")   // Carriage return
            .replace("\t", "\\t")   // Tabs
            // Remove control characters
            // .replace(Regex("[\x00-\x1F\x7F]"), "") TODO
            // Escape JSON protocol delimiters that could confuse the parser
            .replace("}", "\\}")
            .replace("{", "\\{")
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
            // Additional escaping for system prompt context
            .replace("===", "\u200B===\u200B")  // Break protocol markers
            .replace("```", "\u200B```\u200B")  // Break code blocks
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