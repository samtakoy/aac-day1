package com.example.day.core.core_features.mcp.domain.repository

import com.example.day.core.core_features.mcp.domain.model.FileAnalysis

interface FileAnalysisRepository {
    suspend fun getAnalysis(filePath: String): FileAnalysis?
    suspend fun saveAnalysis(filePath: String, content: String)
    suspend fun deleteAnalysis(filePath: String)
}
