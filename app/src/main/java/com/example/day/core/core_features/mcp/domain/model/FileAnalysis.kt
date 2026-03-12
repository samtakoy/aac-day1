package com.example.day.core.core_features.mcp.domain.model

data class FileAnalysis(
    val id: Long,
    val filePath: String,
    val content: String,
    val createdAt: Long
)
