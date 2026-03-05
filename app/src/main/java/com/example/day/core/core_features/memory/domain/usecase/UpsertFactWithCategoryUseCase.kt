package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.memory.domain.repository.LTMGroupRepository
import com.example.day.core.core_features.memory.domain.repository.LongTermMemoryRepository
import javax.inject.Inject

/**
 * UseCase for upserting a fact with category.
 * Coordinates between LTMGroupRepository and LongTermMemoryRepository.
 *
 * This is the primary way to save facts from Workers like PlannerWorker.
 */
class UpsertFactWithCategoryUseCase @Inject constructor(
    private val ltmGroupRepository: LTMGroupRepository,
    private val memoryRepository: LongTermMemoryRepository
) {
    /**
     * Saves or updates a fact for a chat group.
     *
     * @param chatGroupId Chat group ID (will be resolved to LTM group)
     * @param memoryKey Unique key for the fact
     * @param category Category name (stored directly, no separate table)
     * @param fact The fact text to store
     */
    suspend operator fun invoke(
        chatGroupId: Long,
        memoryKey: String,
        category: String,
        fact: String
    ) {
        val ltmGroupId = ltmGroupRepository.findOrCreateByChatGroup(chatGroupId)
        memoryRepository.upsertFact(ltmGroupId, memoryKey, category, fact)
    }

    /**
     * Saves or updates a fact using direct LTM group ID.
     * Use this when you already have the LTM group ID.
     *
     * @param ltmGroupId LTM group ID (direct)
     * @param memoryKey Unique key for the fact
     * @param category Category name (stored directly, no separate table)
     * @param fact The fact text to store
     */
    suspend fun invokeByLTMGroup(
        ltmGroupId: Long,
        memoryKey: String,
        category: String,
        fact: String
    ) {
        memoryRepository.upsertFact(ltmGroupId, memoryKey, category, fact)
    }
}
