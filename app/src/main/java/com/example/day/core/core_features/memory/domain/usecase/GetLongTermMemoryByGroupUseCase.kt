package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact
import com.example.day.core.core_features.memory.domain.repository.LongTermMemoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for retrieving LTM facts by LTM Group ID (direct).
 * For retrieving facts by ChatGroup ID, use GetFactsByChatGroupUseCase instead.
 */
class GetLongTermMemoryByGroupUseCase @Inject constructor(
    private val repository: LongTermMemoryRepository
) {
    /**
     * Gets facts for an LTM group as a Flow.
     *
     * @param ltmGroupId LTM Group ID (direct, not ChatGroup ID)
     */
    operator fun invoke(ltmGroupId: Long): Flow<List<LongTermMemoryFact>> =
        repository.getFactsByGroupFlow(ltmGroupId)
}
