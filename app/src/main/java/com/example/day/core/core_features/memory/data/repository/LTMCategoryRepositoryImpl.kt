package com.example.day.core.core_features.memory.data.repository

import com.example.day.core.core_features.memory.data.local.dao.LTMCategoryDao
import com.example.day.core.core_features.memory.data.local.mapper.LTMCategoryMapper
import com.example.day.core.core_features.memory.domain.model.LTMCategory
import com.example.day.core.core_features.memory.domain.repository.LTMCategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class LTMCategoryRepositoryImpl @Inject constructor(
    private val categoryDao: LTMCategoryDao
) : LTMCategoryRepository {

    override suspend fun ensureCategory(title: String): Long {
        return categoryDao.getOrCreate(title)
    }

    override suspend fun ensureCategories(titles: List<String>): Map<String, Long> {
        return categoryDao.getOrCreateBatch(titles)
    }

    override suspend fun getById(id: Long): LTMCategory? {
        return categoryDao.getById(id)?.let { LTMCategoryMapper.toDomain(it) }
    }

    override suspend fun getByTitle(title: String): LTMCategory? {
        return categoryDao.getByTitle(title.lowercase())?.let { LTMCategoryMapper.toDomain(it) }
    }

    override fun getAll(): Flow<List<LTMCategory>> {
        return categoryDao.getAll().map { entities ->
            LTMCategoryMapper.toDomainList(entities)
        }
    }

    override suspend fun deleteById(id: Long) {
        categoryDao.deleteById(id)
    }
}
