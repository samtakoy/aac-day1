package com.example.day.core.core_features.mcp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.day.core.core_features.mcp.data.local.entity.FileAnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FileAnalysisDao {
    
    @Query("SELECT * FROM file_analysis WHERE file_path = :filePath")
    suspend fun getAnalysis(filePath: String): FileAnalysisEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: FileAnalysisEntity)
    
    @Query("DELETE FROM file_analysis WHERE file_path = :filePath")
    suspend fun deleteAnalysis(filePath: String)
    
    @Query("SELECT * FROM file_analysis")
    fun getAllAnalysesAsFlow(): Flow<List<FileAnalysisEntity>>
}
