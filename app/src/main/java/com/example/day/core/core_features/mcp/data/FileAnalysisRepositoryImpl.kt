package com.example.day.core.core_features.mcp.data

import com.example.day.core.core_features.mcp.data.local.dao.FileAnalysisDao
import com.example.day.core.core_features.mcp.data.mapper.FileAnalysisMapper
import com.example.day.core.core_features.mcp.domain.model.FileAnalysis
import com.example.day.core.core_features.mcp.domain.repository.FileAnalysisRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FileAnalysisRepositoryImpl @Inject constructor(
    private val dao: FileAnalysisDao
) : FileAnalysisRepository {
    
    override suspend fun getAnalysis(filePath: String): FileAnalysis? =
        dao.getAnalysis(filePath)?.let(FileAnalysisMapper::toDomain)
    
    override suspend fun saveAnalysis(filePath: String, content: String) {
        val analysis = FileAnalysis(
            id = 0,
            filePath = filePath,
            content = content,
            createdAt = System.currentTimeMillis()
        )
        dao.insertAnalysis(FileAnalysisMapper.toEntity(analysis))
    }
    
    override suspend fun deleteAnalysis(filePath: String) {
        dao.deleteAnalysis(filePath)
    }
}
