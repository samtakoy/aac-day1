package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.memory.domain.model.UserProfile
import com.example.day.core.core_features.memory.domain.repository.UserProfileRepository
import javax.inject.Inject

class GetAllProfilesUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(): List<UserProfile> = repository.getAllProfiles()
}
