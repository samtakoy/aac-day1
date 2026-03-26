package com.example.day.core.core_features.memory.domain.provider

import android.util.Log
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.agent.domain.model.PromptMessages
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import com.example.day.core.core_features.memory.domain.provider.rag.RagLog
import com.example.day.core.core_features.memory.domain.provider.rag.RagSearchRepository
import javax.inject.Inject

/**
 * Memory provider that enriches the user's prompt with RAG-server search results.
 *
 * Activated when [com.example.day.core.core_features.memory.domain.provider.base.MemoryType.AutoRag]
 * is added to the agent's memory types via `@@talk(rag --on)`.
 *
 * Server URL is stored in AgentMemoryRepository (key=[MEMORY_KEY], category=[CATEGORY_URL]).
 * Falls back to [DEFAULT_URL] if not configured.
 *
 * [setContext] — вызывается из RagContextMemoryProvider перед appendUserPrompt()
 * для передачи TaskState и Short History в QueryOptimizer на сервере.
 *
 * On error (server unavailable, index not ready) silently returns the original prompt unchanged.
 */
class AutoRagMemoryProvider @Inject constructor(
    private val agentMemoryRepository: AgentMemoryRepository,
    private val ragSearchRepository: RagSearchRepository
) : MemoryProvider {

    private var agentId: Long? = null
    private var taskStateJson: String? = null
    private var shortHistory: String? = null

    fun bindAgentId(agentId: Long) {
        this.agentId = agentId
    }

    /** Устанавливается из RagContextMemoryProvider перед вызовом appendUserPrompt(). */
    fun setContext(taskStateJson: String?, shortHistory: String?) {
        this.taskStateJson = taskStateJson
        this.shortHistory = shortHistory
    }

    override suspend fun getMemoryContext(): List<AContextMessage> = emptyList()

    override suspend fun appendUserPrompt(prompt: AContextMessage): PromptMessages {
        val agentId = agentId ?: run {
            Log.e(RagLog.TAG, "AutoRag: agentId IS NULL — search SKIPPED for '${prompt.content.take(60)}'")
            return PromptMessages(prompt = prompt)
        }

        val serverUrl = agentMemoryRepository
            .getFact(agentId, MEMORY_KEY, CATEGORY_URL)?.fact
            ?: DEFAULT_URL

        Log.d(RagLog.TAG, "RAG search: query='${prompt.content.take(80)}', preset=reranked_llm, hasTaskState=${!taskStateJson.isNullOrBlank()}, hasHistory=${!shortHistory.isNullOrBlank()}")

        val ragResult = ragSearchRepository.search(
            query = prompt.content,
            serverUrl = serverUrl,
            taskStateJson = null, // taskStateJson, ПОКА ОТКАЗАЛСЯ, т.к. только вредит
            shortHistory = shortHistory,
            preset = "reranked_llm",
        ).getOrElse { e ->
            Log.e(RagLog.TAG, "RAG search FAILED: ${e.javaClass.simpleName}: ${e.message}")
            return PromptMessages(prompt = prompt)
        }

        Log.d(RagLog.TAG, "RAG raw response (${ragResult.length} chars): '${ragResult.take(200)}'")

        if (ragResult.isBlank()) return PromptMessages(prompt = prompt)

        Log.d(RagLog.TAG, "RAG search ok: result length=${ragResult.length}")

        val contextMessage = AContextMessage(
            role = AContextMessage.Role.USER,
            content = "Контекстная информация по запросу:\n$ragResult",
        )
        return PromptMessages(context = listOf(contextMessage), prompt = prompt)
    }

    companion object {
        const val MEMORY_KEY = "settings"
        const val CATEGORY_URL = "rag_server_url"
        const val DEFAULT_URL = "http://10.0.2.2:3001"
    }
}
