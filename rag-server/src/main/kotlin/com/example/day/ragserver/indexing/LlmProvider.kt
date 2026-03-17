package com.example.day.ragserver.indexing

fun interface LlmProvider {
    suspend fun generate(prompt: String): String
}
