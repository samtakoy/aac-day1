package com.example.day.core.core_features.memory.domain.usecase

import com.example.day.core.core_features.memory.domain.repository.LTMCategoryRepository
import javax.inject.Inject

/**
 * UseCase for ensuring a category exists in the database.
 * Lazily creates the category if it doesn't exist.
 * Title is normalized to lowercase for consistent storage.
 */
class EnsureCategoryUseCase @Inject constructor(
    private val categoryRepository: LTMCategoryRepository
) {
    /**
     * Gets existing category ID or creates new one if not exists.
     *
     * @param title Category title (will be normalized to lowercase)
     * @return Category ID
     */
    suspend operator fun invoke(title: String): Long {
        return categoryRepository.ensureCategory(title)
    }

    /**
     * Batch operation to get or create multiple categories.
     *
     * @param titles List of category titles
     * @return Map of original titles to their IDs
     */
    suspend fun invokeBatch(titles: List<String>): Map<String, Long> {
        return categoryRepository.ensureCategories(titles)
    }
}
