package com.example.day.core.core_features.memory.domain.repository

import com.example.day.core.core_features.memory.domain.model.LTMCategory
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for LTM category operations.
 */
interface LTMCategoryRepository {

    /**
     * Gets existing category ID or creates new one if not exists.
     * Title is normalized to lowercase for consistent storage.
     *
     * @param title Category title (will be normalized to lowercase)
     * @return Category ID
     */
    suspend fun ensureCategory(title: String): Long

    /**
     * Batch operation to get or create multiple categories.
     *
     * @param titles List of category titles
     * @return Map of original titles to their IDs
     */
    suspend fun ensureCategories(titles: List<String>): Map<String, Long>

    /**
     * Get category by ID.
     */
    suspend fun getById(id: Long): LTMCategory?

    /**
     * Get category by title (normalized lookup).
     */
    suspend fun getByTitle(title: String): LTMCategory?

    /**
     * Get all categories as Flow.
     */
    fun getAll(): Flow<List<LTMCategory>>

    /**
     * Delete category by ID.
     */
    suspend fun deleteById(id: Long)
}
