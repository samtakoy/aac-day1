package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.memory.domain.repository.LongTermMemoryRepository
import com.example.day.core.core_features.memory.domain.repository.LTMGroupRepository
import javax.inject.Inject

/**
 * UseCase for upserting a fact for an agent with automatic category resolution.
 * Coordinates between EnsureCategoryUseCase and LongTermMemoryRepository.
 */
class UpsertFactWithCategoryForAgentUseCase @Inject constructor(
    private val ltmGroupRepository: LTMGroupRepository,
    private val ensureCategoryUseCase: EnsureCategoryUseCase,
    private val memoryRepository: LongTermMemoryRepository
) {
    /**
     * Saves or updates a fact for an agent.
     * Automatically resolves/creates the LTM group and category.
     *
     * @param agentId Agent ID (will be resolved to LTM group)
     * @param memoryKey Unique key for the fact
     * @param categoryTitle Category title (will be normalized and created if needed)
     * @param fact The fact text to store
     */
    suspend operator fun invoke(
        agentId: Long,
        memoryKey: String,
        categoryTitle: String,
        fact: String
    ) {
        val ltmGroupId = ltmGroupRepository.findOrCreateByAgent(agentId)
        val categoryId = ensureCategoryUseCase(categoryTitle)
        memoryRepository.upsertFact(ltmGroupId, memoryKey, categoryId, fact)
    }

    /**
     * Saves or updates a fact using direct LTM group ID.
     * Use this when you already have the LTM group ID.
     *
     * @param ltmGroupId LTM group ID (direct)
     * @param memoryKey Unique key for the fact
     * @param categoryTitle Category title (will be normalized and created if needed)
     * @param fact The fact text to store
     */
    suspend fun invokeByLTMGroup(
        ltmGroupId: Long,
        memoryKey: String,
        categoryTitle: String,
        fact: String
    ) {
        val categoryId = ensureCategoryUseCase(categoryTitle)
        memoryRepository.upsertFact(ltmGroupId, memoryKey, categoryId, fact)
    }
}
