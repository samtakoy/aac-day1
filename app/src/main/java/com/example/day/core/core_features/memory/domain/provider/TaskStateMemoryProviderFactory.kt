package com.example.day.core.core_features.memory.domain.provider

import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.state_machine.domain.StateStore
import javax.inject.Inject

/**
 * Stateless factory for creating TaskStateMemoryProvider instances.
 * StateStore is passed as a parameter to create() to support multiple workers.
 */
class StateMemoryProviderFactory @Inject constructor() {
    fun create(chat: Chat, agentId: Long, stateStore: StateStore): TaskStateMemoryProvider {
        return TaskStateMemoryProvider(
            chat = chat,
            agentId = agentId,
            taskStateStore = stateStore
        )
    }
}
