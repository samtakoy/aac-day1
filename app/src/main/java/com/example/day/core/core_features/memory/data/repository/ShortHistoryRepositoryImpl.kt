package com.example.day.core.core_features.memory.data.repository

import android.util.Log
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.memory.domain.provider.rag.RagLog
import com.example.day.core.core_features.memory.domain.provider.rag.ShortHistoryEntry
import com.example.day.core.core_features.memory.domain.provider.rag.ShortHistoryRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ShortHistoryRepositoryImpl @Inject constructor(
    private val agentMemoryRepository: AgentMemoryRepository,
    private val json: Json,
) : ShortHistoryRepository {

    companion object {
        private val TAG = RagLog.TAG
        const val CATEGORY = "short_history"
        const val KEY = "entries"
        const val MAX_ENTRIES = 6
    }

    override suspend fun append(agentId: Long, userMessage: String, assistantSummary: String) {
        val current = getEntries(agentId).toMutableList()
        current.add(ShortHistoryEntry(userMessage, assistantSummary))
        if (current.size > MAX_ENTRIES) current.removeAt(0)

        val serialized = json.encodeToString(
            ListSerializer(ShortHistoryEntryDto.serializer()),
            current.map { ShortHistoryEntryDto(it.userMessage, it.assistantSummary) }
        )
        agentMemoryRepository.upsertFact(agentId, KEY, CATEGORY, serialized)
        Log.d(TAG, "ShortHistory: appended entry, total=${current.size}")
    }

    override suspend fun getAsText(agentId: Long): String =
        getEntries(agentId).joinToString("\n") { "USER: ${it.userMessage}\nASSISTANT: ${it.assistantSummary}" }

    override suspend fun getEntries(agentId: Long): List<ShortHistoryEntry> {
        val raw = agentMemoryRepository.getFact(agentId, KEY, CATEGORY)?.fact ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(ShortHistoryEntryDto.serializer()), raw)
                .map { ShortHistoryEntry(it.userMessage, it.assistantSummary) }
        }.getOrElse {
            Log.w(TAG, "ShortHistory: failed to parse entries: ${it.message}")
            emptyList()
        }
    }

    @Serializable
    private data class ShortHistoryEntryDto(
        val userMessage: String,
        val assistantSummary: String,
    )
}
