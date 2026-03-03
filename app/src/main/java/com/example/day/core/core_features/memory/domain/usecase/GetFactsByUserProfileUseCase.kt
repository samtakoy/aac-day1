package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.memory.domain.model.LongTermMemoryFact
import com.example.day.core.core_features.memory.domain.repository.LongTermMemoryRepository
import javax.inject.Inject

/**
 * Retrieves all LTM facts for a user profile via its ltmGroupId.
 */
class GetFactsByUserProfileUseCase @Inject constructor(
    private val memoryRepository: LongTermMemoryRepository,
    private val getCurrentUserProfile: GetCurrentUserProfileUseCase
) {
    suspend operator fun invoke(): List<LongTermMemoryFact> {
        val profile = getCurrentUserProfile() ?: return emptyList()
        return memoryRepository.getFactsByGroup(profile.ltmGroupId)
    }
}
