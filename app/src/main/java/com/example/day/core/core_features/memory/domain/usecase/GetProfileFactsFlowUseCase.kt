package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact
import com.example.day.core.core_features.memory.domain.repository.LongTermMemoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProfileFactsFlowUseCase @Inject constructor(
    private val repository: LongTermMemoryRepository
) {
    operator fun invoke(ltmGroupId: Long): Flow<List<LongTermMemoryFact>> =
        repository.getFactsByGroupFlow(ltmGroupId)
}
