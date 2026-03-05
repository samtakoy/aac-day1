package com.example.day.core.core_features.memory.domain.usecase

import javax.inject.Inject

/**
 * Adds or updates a fact in the current user's profile memory.
 * Input format: "memoryKey:category:factText"
 * The category part is everything between first and second colon.
 * The fact text may contain colons.
 */
class UpsertFactForProfileUseCase @Inject constructor(
    private val getCurrentUserProfile: GetCurrentUserProfileUseCase,
    private val upsertFactWithCategory: UpsertFactWithCategoryUseCase
) {
    /**
     * Если [memoryKey] == "" то факты уйдут в категорию "User"
     * */
    suspend operator fun invoke(memoryKey: String, category: String, fact: String): Result<Unit> {
        val category = category.trim()
        val fact = fact.trim()
        val memoryKey = if (memoryKey.isBlank()) {
            // по умолчанию факты о пользователе
            DEFAULT_PROFILE_KEY
        } else {
            memoryKey
        }

        if (category.isBlank() || fact.isBlank()) {
            return Result.failure(IllegalArgumentException("category и текст не могут быть пустыми"))
        }


        val profile = getCurrentUserProfile()
            ?: return Result.failure(IllegalStateException("Профиль не привязан. Используйте @@talk(profile --bind NAME)"))

        upsertFactWithCategory.invokeByLTMGroup(
            ltmGroupId = profile.ltmGroupId,
            memoryKey = memoryKey,
            category = category,
            fact = fact
        )
        return Result.success(Unit)
    }

    companion object {
        const val DEFAULT_PROFILE_KEY = "User"
    }
}
