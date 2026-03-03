package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact
import com.example.day.core.core_features.memory.domain.repository.LTMGroupRepository
import com.example.day.core.core_features.memory.domain.repository.LongTermMemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * UseCase for retrieving LTM facts by ChatGroup ID.
 * Coordinates between LTMGroupRepository (for link lookup/creation) and LongTermMemoryRepository.
 * This maintains Clean Architecture by keeping repository dependencies in the UseCase layer.
 *
 * @param chatGroupId ID of the chat group to retrieve facts for
 * @return List of LongTermMemory facts for the linked LTM group
 */
class GetFactsByChatGroupUseCase @Inject constructor(
    private val ltmGroupRepository: LTMGroupRepository,
    private val memoryRepository: LongTermMemoryRepository
) {
    /**
     * Gets facts for a chat group as a one-time operation.
     * Lazily creates LTM group and link if they don't exist.
     */
    suspend operator fun invoke(chatGroupId: Long): List<LongTermMemoryFact> {
        val ltmGroupId = ltmGroupRepository.findOrCreateByChatGroup(chatGroupId)
        return memoryRepository.getFactsByGroup(ltmGroupId)
    }

    /**
     * Gets facts for a chat group as a Flow.
     * Note: This emits whenever facts change, but the link lookup is done once.
     */
    operator fun invoke(chatGroupId: Long, asFlow: Boolean): Flow<List<LongTermMemoryFact>> = flow {
        val ltmGroupId = ltmGroupRepository.findOrCreateByChatGroup(chatGroupId)
        memoryRepository.getFactsByGroupFlow(ltmGroupId).collect { facts ->
            emit(facts)
        }
    }
}
