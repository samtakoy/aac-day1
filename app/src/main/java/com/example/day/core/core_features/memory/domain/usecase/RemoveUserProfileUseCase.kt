package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.memory.domain.repository.UserProfileRepository
import javax.inject.Inject

class RemoveUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(name: String): Result<Unit> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Имя профиля не может быть пустым"))
        repository.getByName(name)
            ?: return Result.failure(IllegalArgumentException("Профиль '$name' не найден"))
        repository.deleteByName(name)
        return Result.success(Unit)
    }
}
