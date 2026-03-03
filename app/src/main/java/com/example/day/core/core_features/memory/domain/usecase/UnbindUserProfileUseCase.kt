package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.chat.domain.ChatRepository
import com.example.day.core.core_features.memory.domain.repository.UserProfileRepository
import javax.inject.Inject

class UnbindUserProfileUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val profileRepository: UserProfileRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val (humanUser, _) = chatRepository.getOrCreateDefaultUsers()
        profileRepository.unbindFromUser(humanUser.id)
        return Result.success(Unit)
    }
}
