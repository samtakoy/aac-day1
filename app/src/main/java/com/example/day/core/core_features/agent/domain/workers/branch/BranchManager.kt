package com.example.day.core.core_features.agent.domain.workers.branch

import com.example.day.core.core_features.agent.domain.AgentContextRepository
import com.example.day.core.core_features.agent.domain.strategy.impl.ContextBranchingStrategy
import javax.inject.Inject
import javax.inject.Provider

/**
 * Encapsulates all branch-related operations.
 */
class BranchManager @Inject constructor(
    private val contextRepository: AgentContextRepository,
    private val strategyProvider: Provider<ContextBranchingStrategy>
) {

    suspend fun createBranch(agentId: Long, branchId: String): BranchResult {
        val strategy = strategyProvider.get()
        val success = strategy.createBranch(agentId, branchId, contextRepository)
        return if (success) {
            BranchResult.Success("Создана новая ветка: $branchId")
        } else {
            BranchResult.Error("Ветка $branchId уже существует")
        }
    }

    suspend fun switchBranch(agentId: Long, branchId: String): BranchResult {
        val strategy = strategyProvider.get()
        val success = strategy.switchBranch(agentId, branchId, contextRepository)
        return if (success) {
            BranchResult.Success("Переключено на ветку: $branchId")
        } else {
            BranchResult.Error("Ветка $branchId не существует")
        }
    }

    suspend fun deleteBranch(agentId: Long, branchId: String): BranchResult {
        val strategy = strategyProvider.get()
        val success = strategy.deleteBranch(agentId, branchId, contextRepository)
        return if (success) {
            BranchResult.Success("Ветка удалена: $branchId")
        } else {
            BranchResult.Error("Не удалось удалить ветку $branchId (возможно, это текущая ветка)")
        }
    }

    suspend fun listBranches(agentId: Long): String {
        val strategy = strategyProvider.get()
        return strategy.listBranches(agentId, contextRepository)
    }
}
