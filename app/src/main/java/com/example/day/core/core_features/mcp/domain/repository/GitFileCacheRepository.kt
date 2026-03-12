package com.example.day.core.core_features.mcp.domain.repository

import com.example.day.core.core_features.mcp.domain.model.GitFileCache
import kotlinx.coroutines.flow.Flow

interface GitFileCacheRepository {
    suspend fun getCachedFileList(): GitFileCache?
    fun getCachedFileListAsFlow(): Flow<GitFileCache?>
    suspend fun cacheFileList(fileList: List<String>, ttlMinutes: Long)
    suspend fun clearCache()
    suspend fun isCacheValid(): Boolean
}
