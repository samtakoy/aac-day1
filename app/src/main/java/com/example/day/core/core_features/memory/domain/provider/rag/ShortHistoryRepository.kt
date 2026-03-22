package com.example.day.core.core_features.memory.domain.provider.rag

interface ShortHistoryRepository {
    /** Добавить запись в short history. Хранит последние MAX_ENTRIES записей. */
    suspend fun append(agentId: Long, userMessage: String, assistantSummary: String)
    /** Получить текущую историю в виде строки "USER: ...\nASSISTANT: ..." */
    suspend fun getAsText(agentId: Long): String
    /** Получить raw список записей */
    suspend fun getEntries(agentId: Long): List<ShortHistoryEntry>
}

data class ShortHistoryEntry(
    val userMessage: String,
    val assistantSummary: String,
)
