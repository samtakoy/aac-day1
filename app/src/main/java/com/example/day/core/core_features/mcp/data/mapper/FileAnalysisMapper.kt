package com.example.day.core.core_features.mcp.data.mapper

import com.example.day.core.core_features.mcp.data.local.entity.FileAnalysisEntity
import com.example.day.core.core_features.mcp.domain.model.FileAnalysis

internal object FileAnalysisMapper {
    fun toDomain(entity: FileAnalysisEntity): FileAnalysis = FileAnalysis(
        id = entity.id,
        filePath = entity.filePath,
        content = entity.content,
        createdAt = entity.createdAt
    )
    
    fun toEntity(domain: FileAnalysis): FileAnalysisEntity = FileAnalysisEntity(
        id = domain.id,
        filePath = domain.filePath,
        content = domain.content,
        createdAt = domain.createdAt
    )
}
