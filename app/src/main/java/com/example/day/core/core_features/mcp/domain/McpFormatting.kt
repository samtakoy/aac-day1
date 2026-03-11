package com.example.day.core.core_features.mcp.domain

import kotlinx.serialization.json.Json

object McpFormatting {
    fun formatResult(raw: String, json: Json): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            val pretty = runCatching {
                val element = json.parseToJsonElement(trimmed)
                json.encodeToString(element)
            }.getOrNull()
            if (pretty != null) return pretty
        }
        return raw
    }
}
