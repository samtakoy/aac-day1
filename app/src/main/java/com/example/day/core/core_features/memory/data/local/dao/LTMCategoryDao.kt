package com.example.day.core.core_features.memory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.day.core.core_features.memory.data.local.model.LTMCategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for LTM Category operations.
 * Provides category management with lowercase title indexing.
 */
@Dao
internal interface LTMCategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: LTMCategoryEntity): Long

    @Query("SELECT * FROM ltm_categories WHERE id = :id")
    suspend fun getById(id: Long): LTMCategoryEntity?

    @Query("SELECT * FROM ltm_categories WHERE title = :title")
    suspend fun getByTitle(title: String): LTMCategoryEntity?

    @Query("SELECT id FROM ltm_categories WHERE title = :title")
    suspend fun getIdByTitle(title: String): Long?

    @Query("SELECT * FROM ltm_categories ORDER BY title ASC")
    fun getAll(): Flow<List<LTMCategoryEntity>>

    @Query("DELETE FROM ltm_categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Gets existing category ID or creates new one if not exists.
     * Title is normalized to lowercase for consistent lookup.
     */
    suspend fun getOrCreate(title: String): Long {
        val normalizedTitle = title.lowercase()
        val existingId = getIdByTitle(normalizedTitle)
        if (existingId != null) {
            return existingId
        }
        return insert(LTMCategoryEntity(title = normalizedTitle))
    }

    /**
     * Batch operation to get or create multiple categories.
     * Returns a map of original titles to their IDs.
     */
    suspend fun getOrCreateBatch(titles: List<String>): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        for (title in titles) {
            val id = getOrCreate(title)
            result[title] = id
        }
        return result
    }
}
