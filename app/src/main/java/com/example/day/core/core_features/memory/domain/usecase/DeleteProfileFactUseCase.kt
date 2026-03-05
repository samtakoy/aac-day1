package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.memory.domain.repository.LongTermMemoryRepository
import javax.inject.Inject

class DeleteProfileFactUseCase @Inject constructor(
    private val repository: LongTermMemoryRepository
) {
    /**
     * Delete a fact by its unique id.
     */
    suspend operator fun invoke(id: Long) =
        repository.deleteFact(id)

    /**
     * Delete a fact by composite key (ltmGroupId + memoryKey + category).
     */
    suspend operator fun invoke(ltmGroupId: Long, memoryKey: String, category: String) =
        repository.deleteFact(ltmGroupId, memoryKey, category)
}
