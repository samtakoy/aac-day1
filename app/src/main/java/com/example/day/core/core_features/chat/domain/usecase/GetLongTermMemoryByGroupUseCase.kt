package com.example.day.core.core_features.chat.domain.usecase

import com.example.day.core.core_features.chat.domain.model.LongTermMemory
import com.example.day.core.core_features.chat.domain.repository.LongTermMemoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLongTermMemoryByGroupUseCase @Inject constructor(
    private val repository: LongTermMemoryRepository
) {
    operator fun invoke(groupId: Long): Flow<List<LongTermMemory>> =
        repository.getFactsByGroupFlow(groupId)
}
