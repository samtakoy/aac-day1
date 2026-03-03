package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.memory.domain.model.UserProfile
import com.example.day.core.core_features.memory.domain.repository.UserProfileRepository
import javax.inject.Inject

class CreateUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(name: String): Result<UserProfile> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Имя профиля не может быть пустым"))
        val existing = repository.getByName(name)
        if (existing != null) return Result.failure(IllegalArgumentException("Профиль с именем '$name' уже существует"))
        return Result.success(repository.createProfile(name))
    }
}
