package com.example.day.core.core_features.mcp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.day.core.core_features.mcp.data.local.entity.GitFileCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface GitFileCacheDao {
    
    @Query("SELECT * FROM git_file_cache ORDER BY id ASC LIMIT 1")
    suspend fun getCachedFileList(): GitFileCacheEntity?
    
    @Query("SELECT * FROM git_file_cache ORDER BY id ASC LIMIT 1")
    fun getCachedFileListAsFlow(): Flow<GitFileCacheEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: GitFileCacheEntity)
    
    @Query("DELETE FROM git_file_cache")
    suspend fun clearCache()
}
