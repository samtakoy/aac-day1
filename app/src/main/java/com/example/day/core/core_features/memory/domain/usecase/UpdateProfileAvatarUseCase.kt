package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.memory.domain.repository.UserProfileRepository
import javax.inject.Inject

class UpdateProfileAvatarUseCase @Inject constructor(
    private val getCurrentUserProfile: GetCurrentUserProfileUseCase,
    private val repository: UserProfileRepository
) {
    suspend fun reset(): Result<Unit> {
        val profile = getCurrentUserProfile()
            ?: return Result.failure(IllegalStateException("Профиль не привязан"))
        repository.updateTextAvatar(profile.id, null)
        return Result.success(Unit)
    }

    suspend fun update(avatar: String): Result<Unit> {
        val profile = getCurrentUserProfile()
            ?: return Result.failure(IllegalStateException("Профиль не привязан"))
        repository.updateTextAvatar(profile.id, avatar)
        return Result.success(Unit)
    }
}
