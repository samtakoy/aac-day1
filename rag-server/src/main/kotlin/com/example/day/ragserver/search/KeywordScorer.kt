package com.example.day.ragserver.search

object KeywordScorer {

    fun score(query: String, text: String): Double {
        val tokens = tokenize(query)
        if (tokens.isEmpty()) return 0.0
        val textLower = text.lowercase()
        val hits = tokens.count { textLower.contains(it) }
        return hits.toDouble() / tokens.size
    }

    /**
     * Splits text into lowercase tokens.
     * Handles camelCase: "createUser" → ["create", "user"]
     * Handles snake_case, dots, brackets, etc.
     */
    private fun tokenize(text: String): List<String> = text
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .lowercase()
        .split(Regex("[\\s\\-_.,:;()\\[\\]{}]+"))
        .filter { it.length > 2 }
}
