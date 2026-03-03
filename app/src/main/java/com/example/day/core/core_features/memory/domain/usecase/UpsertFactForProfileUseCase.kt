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
    suspend operator fun invoke(rawFact: String): Result<Unit> {
        val parts = rawFact.split(":")
        if (parts.size < 3) {
            return Result.failure(
                IllegalArgumentException("Неверный формат. Ожидается: key:category:текст_факта")
            )
        }
        val key = parts[0].trim()
        val category = parts[1].trim()
        val text = parts.drop(2).joinToString(":").trim()

        if (key.isBlank() || category.isBlank() || text.isBlank()) {
            return Result.failure(IllegalArgumentException("key, category и текст не могут быть пустыми"))
        }

        val profile = getCurrentUserProfile()
            ?: return Result.failure(IllegalStateException("Профиль не привязан. Используйте @@talk(profile --bind NAME)"))

        upsertFactWithCategory.invokeByLTMGroup(
            ltmGroupId = profile.ltmGroupId,
            memoryKey = key,
            categoryTitle = category,
            fact = text
        )
        return Result.success(Unit)
    }
}
