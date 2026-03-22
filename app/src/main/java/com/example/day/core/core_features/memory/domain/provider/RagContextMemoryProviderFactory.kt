package com.example.day.core.core_features.memory.domain.provider

import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.memory.domain.provider.rag.ShortHistoryRepository
import com.example.day.core.core_features.memory.domain.provider.rag.TaskStateRepository
import javax.inject.Inject

class RagContextMemoryProviderFactory @Inject constructor(
    private val taskStateRepository: TaskStateRepository,
    private val shortHistoryRepository: ShortHistoryRepository,
    private val autoRagMemoryProvider: AutoRagMemoryProvider,
    private val agentMemoryRepository: AgentMemoryRepository,
    private val agentContextRepository: AgentContextRepository,
) {
    fun create(agentId: Long): RagContextMemoryProvider =
        RagContextMemoryProvider(
            taskStateRepository = taskStateRepository,
            shortHistoryRepository = shortHistoryRepository,
            autoRagMemoryProvider = autoRagMemoryProvider,
            agentMemoryRepository = agentMemoryRepository,
            agentContextRepository = agentContextRepository,
        ).also { it.bindAgentId(agentId) }
}
