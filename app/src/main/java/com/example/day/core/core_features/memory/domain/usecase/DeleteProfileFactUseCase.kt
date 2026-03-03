package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.memory.domain.repository.LongTermMemoryRepository
import javax.inject.Inject

class DeleteProfileFactUseCase @Inject constructor(
    private val repository: LongTermMemoryRepository
) {
    suspend operator fun invoke(ltmGroupId: Long, memoryKey: String, categoryId: Long) =
        repository.deleteFact(ltmGroupId, memoryKey, categoryId)
}
