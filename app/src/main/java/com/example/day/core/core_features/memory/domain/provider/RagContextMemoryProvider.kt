package com.example.day.core.core_features.memory.domain.provider

import android.util.Log
import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AContextState
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.agent.domain.model.PromptMessages
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import com.example.day.core.core_features.memory.domain.provider.rag.RagLog
import com.example.day.core.core_features.memory.domain.provider.rag.ShortHistoryRepository
import com.example.day.core.core_features.memory.domain.provider.rag.TaskStateRepository

/**
 * Центральный memory provider для RAG-чата.
 *
 * Оркестрирует в appendUserPrompt():
 * 1. Обновляет TaskState через rag-server (Ollama) с последними сообщениями диалога
 * 2. Делегирует обогащение промпта в AutoRagMemoryProvider
 *
 * В postProcess() (после успешного ответа LLM):
 * 3. Сохраняет запись в short history (userMessage + lastResponseSummary)
 *
 * Хранение в AgentMemoryRepository:
 * - TaskState: category="task_state", key="current"
 * - Short History: category="short_history", key="entries"
 * - RAG URL: category="rag_server_url", key="settings"
 */
class RagContextMemoryProvider(
    private val taskStateRepository: TaskStateRepository,
    private val shortHistoryRepository: ShortHistoryRepository,
    private val autoRagMemoryProvider: AutoRagMemoryProvider,
    private val agentMemoryRepository: AgentMemoryRepository,
    private val agentContextRepository: AgentContextRepository,
) : MemoryProvider {

    private companion object {
        val TAG = RagLog.TAG
        private val MAX_RECENT_MESSAGES = 2
    }

    private var agentId: Long? = null

    // Хранит данные между appendUserPrompt() и postProcess()
    private var pendingUserMessage: String = ""
    private var pendingLastResponseSummary: String = ""

    fun bindAgentId(agentId: Long) {
        this.agentId = agentId
        autoRagMemoryProvider.bindAgentId(agentId)
    }

    suspend fun getTaskStateJson(): String? {
        val agentId = agentId ?: return null
        return taskStateRepository.getCurrentJson(agentId)
    }

    suspend fun getServerUrl(): String {
        val agentId = agentId ?: return AutoRagMemoryProvider.DEFAULT_URL
        return agentMemoryRepository
            .getFact(agentId, AutoRagMemoryProvider.MEMORY_KEY, AutoRagMemoryProvider.CATEGORY_URL)?.fact
            ?: AutoRagMemoryProvider.DEFAULT_URL
    }

    override suspend fun getMemoryContext(): List<AContextMessage> = emptyList()

    override suspend fun appendUserPrompt(prompt: AContextMessage): PromptMessages {
        val agentId = agentId ?: return PromptMessages(prompt = prompt)

        Log.d(TAG, "appendUserPrompt: agentId=$agentId, prompt='${prompt.content.take(80)}'")

        val serverUrl = agentMemoryRepository
            .getFact(agentId, AutoRagMemoryProvider.MEMORY_KEY, AutoRagMemoryProvider.CATEGORY_URL)?.fact
            ?: AutoRagMemoryProvider.DEFAULT_URL

        // История теперь чистая (RAG-контекст не сохраняется), обрезка не нужна
        val recentMessages = getRecentMessages(agentId) + prompt
        Log.d(TAG, "recentMessages roles: ${recentMessages.map { it.role.name.lowercase() }}")

        val taskStateStart = System.currentTimeMillis()
        val taskStateResult = runCatching {
            taskStateRepository.update(
                agentId = agentId,
                lastMessages = recentMessages,
                serverUrl = serverUrl,
            )
        }.getOrElse { e ->
            Log.w(TAG, "TaskState update failed (${System.currentTimeMillis() - taskStateStart}ms): ${e.message}")
            null
        }

        Log.d(TAG, "taskState updated (${System.currentTimeMillis() - taskStateStart}ms): intent=${taskStateResult?.updatedState?.intent}, focus=${taskStateResult?.updatedState?.currentFocus?.className}, switched=${taskStateResult?.updatedState?.contextSwitched}")

        pendingUserMessage = prompt.content
        pendingLastResponseSummary = taskStateResult?.lastResponseSummary ?: ""

        // Передать TaskState и Short History в AutoRag для QueryOptimizer на сервере
        val taskStateJson = taskStateRepository.getCurrentJson(agentId)
        val shortHistoryEntries = shortHistoryRepository.getEntries(agentId).takeLast(3)
        val historyText = shortHistoryEntries
            .joinToString("\n") { "USER: ${it.userMessage}\nASSISTANT: ${it.assistantSummary}" }
            .takeIf { it.isNotBlank() }
        Log.d(TAG, "shortHistory: ${shortHistoryEntries.size} entries, assistantSummaryLengths=${shortHistoryEntries.map { it.assistantSummary.length }}")

        autoRagMemoryProvider.setContext(
            taskStateJson = taskStateJson,
            shortHistory = historyText,
        )

        val result = autoRagMemoryProvider.appendUserPrompt(prompt)
        Log.d(TAG, "autoRag done: context length=${result.context.sumOf { it.content.length }}, hasContext=${result.context.isNotEmpty()}")
        return result
    }

    /**
     * Вызывается после успешного ответа LLM.
     * Сохраняет запись в short history.
     */
    suspend fun postProcess() {
        val agentId = agentId ?: return
        if (pendingUserMessage.isBlank()) return

        shortHistoryRepository.append(
            agentId = agentId,
            userMessage = pendingUserMessage,
            assistantSummary = pendingLastResponseSummary,
        )
        Log.d(TAG, "postProcess: saved short history entry (summary length=${pendingLastResponseSummary.length})")

        pendingUserMessage = ""
        pendingLastResponseSummary = ""
    }

    /**
     * Для команды debuginfo — возвращает текущее состояние TaskState и Short History.
     */
    suspend fun getDebugInfo(): String {
        val agentId = agentId ?: return "agentId не установлен"
        val taskState = taskStateRepository.getCurrent(agentId)
        val entries = shortHistoryRepository.getEntries(agentId)
        val shortHistoryText = shortHistoryRepository.getAsText(agentId)

        return buildString {
            appendLine("=== TaskState ===")
            appendLine("focus: ${taskState.currentFocus.file} / ${taskState.currentFocus.className} / ${taskState.currentFocus.method}")
            appendLine("intent: ${taskState.intent}")
            appendLine("context_switched: ${taskState.contextSwitched}")
            appendLine("tech_stack: ${taskState.techStack}")
            if (taskState.confirmedDecisions.isNotEmpty()) {
                appendLine("decisions: ${taskState.confirmedDecisions.joinToString("; ")}")
            }
            if (taskState.openQuestions.isNotEmpty()) {
                appendLine("open: ${taskState.openQuestions.joinToString("; ")}")
            }
            appendLine()
            appendLine("=== Short History (${entries.size} записей) ===")
            appendLine(shortHistoryText.ifBlank { "(пусто)" })
        }
    }
// TODO надо возвращать только USER и assistant а не все подряд
    private suspend fun getRecentMessages(agentId: Long): List<AContextMessage> {
        return when (val state = agentContextRepository.getContextState(agentId)) {
            is AContextState.Summary -> state.messages.takeLast(MAX_RECENT_MESSAGES)
            is AContextState.SlidingWindow -> state.messages.takeLast(MAX_RECENT_MESSAGES)
            is AContextState.Full -> state.messages.takeLast(MAX_RECENT_MESSAGES)
            is AContextState.StickyFacts -> state.messages.takeLast(MAX_RECENT_MESSAGES)
            else -> emptyList()
        }
    }
}
