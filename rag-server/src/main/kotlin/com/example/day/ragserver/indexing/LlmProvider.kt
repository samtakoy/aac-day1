package com.example.day.ragserver.indexing

interface LlmProvider {
    val modelName: String
    suspend fun generate(prompt: String): String
}
