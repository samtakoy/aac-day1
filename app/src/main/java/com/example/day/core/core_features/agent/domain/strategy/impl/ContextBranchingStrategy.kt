package com.example.day.core.core_features.agent.domain.strategy.impl

import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.model.AContextState
import com.example.day.core.core_features.agent.domain.model.AgentConfig
import com.example.day.core.core_features.agent.domain.model.addAssistantMessage
import com.example.day.core.core_features.agent.domain.model.addUserMessage
import com.example.day.core.core_features.agent.domain.strategy.ContextSnapshot
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategy
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyConstants
import com.example.day.core.core_features.agent.domain.strategy.ContextStrategyResult
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap

/**
 * Branching strategy - allows creating independent dialogue branches.
 * Each branch stores its own message history, can switch between branches.
 *
 * Commands:
 * - process(): returns messages from current branch + new user prompt
 * - afterResponse(): adds messages to current branch only
 */
class ContextBranchingStrategy : ContextStrategy {

    companion object {
        const val DEFAULT_BRANCH_ID = "main"
    }

    override suspend fun process(
        agent: AgentConfig,
        store: AgentContextRepository
    ): ContextSnapshot {
        val state = store.getContextState(agent.id) as? AContextState.Branching
            ?: createInitialState()

        // Get messages from current branch
        // НЕ добавляем userPrompt — это сделает LlmRequestUseCase.exec()
        val currentMessages = state.branches[state.currentBranchId] ?: persistentListOf()

        return ContextSnapshot(messages = currentMessages)
    }

    override suspend fun afterResponse(
        agent: AgentConfig,
        response: String,
        store: AgentContextRepository,
        fullContext: ContextSnapshot
    ): ContextStrategyResult {
        val state = store.getContextState(agent.id) as? AContextState.Branching
            ?: createInitialState()

        // fullContext содержит полную историю: initialHistory + prompt + assistant/tool messages
        val messagesToSave = fullContext.messages.toPersistentList()

        // Update branches map
        val updatedBranches = state.branches.toMutableMap().apply {
            put(state.currentBranchId, messagesToSave)
        }

        store.saveContextState(
            agent.id,
            state.copy(branches = updatedBranches.toPersistentMap())
        )

        return ContextStrategyResult(
            "Сохранено в ветку: ${state.currentBranchId} (${messagesToSave.size} сообщений)"
        )
    }

    override suspend fun getInfoReport(
        agent: AgentConfig,
        store: AgentContextRepository
    ): String {
        val params = store.getContextParams(agent.id) as? AContextParams.Branching
        val state = store.getContextState(agent.id) as? AContextState.Branching
            ?: createInitialState()

        val defaultBranch = params?.defaultBranchId ?: DEFAULT_BRANCH_ID
        val currentBranch = state.currentBranchId
        val branchCount = state.branches.size
        val currentMessages = state.branches[currentBranch]?.size ?: 0

        return buildString {
            appendLine("Стратегия: branching (ветвление)")
            appendLine("Основная ветка: $defaultBranch")
            appendLine("Текущая ветка: $currentBranch")
            appendLine("Всего веток: $branchCount")
            append("Сообщений в текущей ветке: $currentMessages")
        }
    }

    override suspend fun getFullContextReport(
        agent: AgentConfig,
        store: AgentContextRepository
    ): String {
        val state = store.getContextState(agent.id) as? AContextState.Branching
            ?: createInitialState()

        return buildString {
            appendLine("=== Агент: ${agent.systemName} ===")
            appendLine("Системный промпт: ${agent.systemPrompt}")
            appendLine()
            appendLine("=== Ветки диалога ===")
            state.branches.forEach { (branchId, messages) ->
                val marker = if (branchId == state.currentBranchId) " [текущая]" else ""
                appendLine("- $branchId: ${messages.size} сообщений$marker")
            }
            appendLine()
            appendLine("=== Сообщения в текущей ветке ${state.currentBranchId} ===")
            val currentMessages = state.branches[state.currentBranchId] ?: persistentListOf()
            currentMessages.forEach { msg ->
                appendLine("[${msg.role}]: ${msg.content.take(100)}...")
            }
        }
    }

    override suspend fun updateParams(
        agent: AgentConfig,
        params: Map<String, String>,
        store: AgentContextRepository
    ): String {
        val defaultBranch = params[ContextStrategyConstants.PARAM_DEFAULT_BRANCH] ?: DEFAULT_BRANCH_ID

        val currentState = store.getContextState(agent.id) as? AContextState.Branching

        if (currentState != null) {
            // Update default branch in state
            store.saveContextState(
                agent.id,
                currentState.copy(defaultBranchId = defaultBranch)
            )
        }

        store.saveContextParams(agent.id, AContextParams.Branching(defaultBranchId = defaultBranch))

        return "Основная ветка обновлена: $defaultBranch"
    }

    /**
     * Create a new branch from current branch (checkpoint).
     * @return true if branch was created successfully
     */
    suspend fun createBranch(
        agentId: Long,
        newBranchId: String,
        store: AgentContextRepository
    ): Boolean {
        val state = store.getContextState(agentId) as? AContextState.Branching
            ?: createInitialState()

        // Check if branch already exists
        if (state.branches.containsKey(newBranchId)) {
            return false
        }

        // Copy current branch messages to new branch
        val currentContent = state.branches[state.currentBranchId] ?: persistentListOf()

        val updatedBranches = state.branches.toMutableMap().apply {
            put(newBranchId, currentContent)
        }

        store.saveContextState(
            agentId,
            state.copy(
                branches = updatedBranches.toPersistentMap(),
                currentBranchId = newBranchId
            )
        )

        return true
    }

    /**
     * Switch to existing branch.
     * @return true if switch was successful, false if branch doesn't exist
     */
    suspend fun switchBranch(
        agentId: Long,
        branchId: String,
        store: AgentContextRepository
    ): Boolean {
        val state = store.getContextState(agentId) as? AContextState.Branching
            ?: createInitialState()

        return if (state.branches.containsKey(branchId)) {
            store.saveContextState(agentId, state.copy(currentBranchId = branchId))
            true
        } else {
            false
        }
    }

    /**
     * Delete a branch.
     * @return true if deletion was successful, false if branch doesn't exist or is current
     */
    suspend fun deleteBranch(
        agentId: Long,
        branchId: String,
        store: AgentContextRepository
    ): Boolean {
        val state = store.getContextState(agentId) as? AContextState.Branching
            ?: return false

        // Can't delete current branch
        if (branchId == state.currentBranchId) {
            return false
        }

        // Branch doesn't exist
        if (!state.branches.containsKey(branchId)) {
            return false
        }

        val updatedBranches = state.branches.toMutableMap().apply {
            remove(branchId)
        }

        store.saveContextState(
            agentId,
            state.copy(branches = updatedBranches.toPersistentMap())
        )

        return true
    }

    /**
     * Get list of all branches with info.
     * @return formatted string with branch list
     */
    suspend fun listBranches(agentId: Long, store: AgentContextRepository): String {
        val state = store.getContextState(agentId) as? AContextState.Branching
            ?: return "Веток пока нет"

        return buildString {
            appendLine("Ветки диалога:")
            state.branches.forEach { (branchId, messages) ->
                val marker = if (branchId == state.currentBranchId) " [текущая]" else ""
                val defaultMarker = if (branchId == state.defaultBranchId) " [основная]" else ""
                appendLine("  - $branchId: ${messages.size} сообщений$marker$defaultMarker")
            }
        }
    }

    private fun createInitialState(): AContextState.Branching {
        return AContextState.Branching(
            branches = persistentMapOf(DEFAULT_BRANCH_ID to persistentListOf()),
            currentBranchId = DEFAULT_BRANCH_ID,
            defaultBranchId = DEFAULT_BRANCH_ID
        )
    }
}
