package com.example.day.core.core_features.memory.domain.provider

import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.agent.domain.workers.task.TaskStateStore
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Factory for creating TaskStateMemoryProvider instances with proper runtime dependencies.
 *
 * This factory solves the DI problem where TaskStateMemoryProvider needs both injectable
 * dependencies (AgentMemoryRepository, TaskStateStore, Json) and runtime values (Chat, agentId).
 *
 * Usage:
 * ```
 * val provider = factory.create(chat, agentId)
 * ```
 */
class TaskStateMemoryProviderFactory @Inject constructor(
    private val agentMemoryRepository: AgentMemoryRepository,
    private val taskStateStore: TaskStateStore,
    private val json: Json
) {
    /**
     * Creates a TaskStateMemoryProvider with the given runtime values.
     *
     * @param chat The chat context (runtime value)
     * @param agentId The agent ID (runtime value)
     * @return A new TaskStateMemoryProvider instance
     */
    fun create(chat: Chat, agentId: Long): TaskStateMemoryProvider {
        return TaskStateMemoryProvider(
            agentMemoryRepository = agentMemoryRepository,
            chat = chat,
            agentId = agentId,
            taskStateStore = taskStateStore,
            json = json
        )
    }
}
