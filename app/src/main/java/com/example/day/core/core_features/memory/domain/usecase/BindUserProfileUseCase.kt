package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.memory.domain.repository.UserProfileRepository
import javax.inject.Inject

class BindUserProfileUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val profileRepository: UserProfileRepository
) {
    suspend operator fun invoke(profileName: String): Result<Unit> {
        val profile = profileRepository.getByName(profileName)
            ?: return Result.failure(IllegalArgumentException("Профиль '$profileName' не найден"))
        val (humanUser, _) = chatRepository.getOrCreateDefaultUsers()
        profileRepository.bindToUser(humanUser.id, profile.id)
        return Result.success(Unit)
    }
}
