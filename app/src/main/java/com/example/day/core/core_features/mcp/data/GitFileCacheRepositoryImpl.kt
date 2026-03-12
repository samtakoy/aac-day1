package com.example.day.core.core_features.mcp.data

import com.example.day.core.core_features.mcp.data.local.dao.GitFileCacheDao
import com.example.day.core.core_features.mcp.data.mapper.GitFileCacheMapper
import com.example.day.core.core_features.mcp.domain.model.GitFileCache
import com.example.day.core.core_features.mcp.domain.repository.GitFileCacheRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class GitFileCacheRepositoryImpl @Inject constructor(
    private val dao: GitFileCacheDao,
) : GitFileCacheRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    override suspend fun getCachedFileList(): GitFileCache? =
        dao.getCachedFileList()?.let { GitFileCacheMapper.toDomain(it, json) }

    override fun getCachedFileListAsFlow(): Flow<GitFileCache?> =
        dao.getCachedFileListAsFlow().map { it?.let { entity -> GitFileCacheMapper.toDomain(entity, json) } }

    override suspend fun cacheFileList(fileList: List<String>, ttlMinutes: Long) {
        val now = System.currentTimeMillis()
        val cache = GitFileCache(
            id = 0,
            fileList = fileList,
            createdAt = now,
            expiresAt = now + (ttlMinutes * 60 * 1000)
        )
        dao.insertCache(GitFileCacheMapper.toEntity(cache, json))
    }

    override suspend fun clearCache() {
        dao.clearCache()
    }

    override suspend fun isCacheValid(): Boolean {
        val cache = getCachedFileList()
        return cache != null && System.currentTimeMillis() < cache.expiresAt
    }
}
