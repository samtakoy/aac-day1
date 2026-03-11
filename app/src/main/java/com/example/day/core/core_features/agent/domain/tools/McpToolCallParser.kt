package com.example.day.core.core_features.agent.domain.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// TODO это в правильном модуле?
object McpToolCallParser {

    data class Parsed(
        val tool: String,
        val arguments: JsonObject,
        val cleanedText: String
    )

    fun tryParse(raw: String, json: Json): Parsed? {
        val trimmed = raw.trim()
        val parsed = decodeObject(trimmed, json)
            ?: decodeFromSubstring(trimmed, json)
            ?: return null

        val tool = parsed["tool"]?.jsonPrimitive?.contentOrNull ?: return null
        val arguments = parsed["arguments"]?.jsonObject ?: JsonObject(emptyMap())
        val cleaned = removeJsonBlock(trimmed)
        return Parsed(tool = tool, arguments = arguments, cleanedText = cleaned)
    }

    private fun decodeObject(raw: String, json: Json): JsonObject? = runCatching {
        val element = json.parseToJsonElement(raw)
        element.jsonObject
    }.getOrNull()

    private fun decodeFromSubstring(raw: String, json: Json): JsonObject? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start == -1 || end <= start) return null
        val candidate = raw.substring(start, end + 1)
        return decodeObject(candidate, json)
    }

    private fun removeJsonBlock(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start == -1 || end <= start) return raw.trim()
        return (raw.removeRange(start, end + 1)).trim()
    }
}
