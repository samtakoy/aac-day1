package com.example.day.core.core_features.memory.domain.provider

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.Role
import com.example.day.core.core_features.agent.domain.model.TaskMemoryKeys
import com.example.day.core.core_features.agent.domain.model.TaskState
import com.example.day.core.core_features.agent.domain.repository.AgentMemoryRepository
import com.example.day.core.core_features.agent.domain.workers.task.TaskContext
import com.example.day.core.core_features.agent.domain.workers.task.TaskStateMachine
import com.example.day.core.core_features.agent.domain.workers.task.prompt.PromptSanitizer
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.memory.domain.provider.base.MemoryProvider
import javax.inject.Inject

/**
 * Factory for creating TaskStateMemoryProvider instances with proper agentId.
 *
 * This class is injectable via DI but requires agentId to be provided at runtime.
 * Use [withAgentId] to create a working MemoryProvider instance.
 */
class TaskStateMemoryProvider @Inject constructor(
    private val agentMemoryRepository: AgentMemoryRepository,
    private val chat: Chat,
    private val agentId: Long
) : MemoryProvider {
    companion object {
        const val SYSTEM_PROMPT_PREFIX = "Ответ давай на русском языке. " +
            "Твой ответ должен быть валидным JSON объектом без markdown разметки, " +
            "следуя схеме в инструкциях.\n\n"
    }

    override suspend fun getMemoryContext(): List<AContextMessage> {
        val facts = agentMemoryRepository.getFacts(agentId)
            .associate { it.memoryKey to it.fact }
        return buildSystemPrompt(facts, agentId)
    }

    /**
     * Builds system prompt based on current task state and facts.
     * This method contains the shared logic used by all provider instances.
     *
     * @param facts Map of memory key to value from LTM
     * @param agentId The agent ID for context
     * @return List containing the system message
     */
    internal fun buildSystemPrompt(
        facts: Map<String, String>,
        agentId: Long
    ): List<AContextMessage> {
        val currentState = determineCurrentState(facts)
        val currentStep = facts[TaskMemoryKeys.CURRENT_STEP]?.toIntOrNull() ?: 1

        val sanitizedFacts = PromptSanitizer.sanitizeMap(facts)

        val context = TaskContext(
            chat = chat,
            agentId = agentId,
            facts = sanitizedFacts,
            currentState = currentState,
            currentStep = currentStep
        )

        val stateMachine = TaskStateMachine()
        val handler = stateMachine.getHandler(currentState)
        val systemPrompt = SYSTEM_PROMPT_PREFIX + handler.buildSystemPrompt(context)

        return listOf(
            AContextMessage(
                role = Role.SYSTEM,
                content = systemPrompt
            )
        )
    }

    private fun determineCurrentState(facts: Map<String, String>): TaskState {
        val stateStr = facts[TaskMemoryKeys.CURRENT_STATE]
        return when {
            stateStr == null || stateStr.isBlank() -> TaskState.INIT
            stateStr.equals("DONE", ignoreCase = true) -> TaskState.INIT
            else -> TaskState.fromString(stateStr) ?: TaskState.INIT
        }
    }
}