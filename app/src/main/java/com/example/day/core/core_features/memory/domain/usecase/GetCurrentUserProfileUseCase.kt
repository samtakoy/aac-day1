package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.memory.domain.model.UserProfile
import com.example.day.core.core_features.memory.domain.repository.UserProfileRepository
import javax.inject.Inject

/**
 * Returns the profile currently bound to the main (human) user.
 * Returns null if no profile is bound.
 */
class GetCurrentUserProfileUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val profileRepository: UserProfileRepository
) {
    suspend operator fun invoke(): UserProfile? {
        val (humanUser, _) = chatRepository.getOrCreateDefaultUsers()
        return profileRepository.getProfileForUser(humanUser.id)
    }
}
