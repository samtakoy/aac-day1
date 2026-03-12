package com.example.day.core.core_features.mcp.domain.model

data class GitFileCache(
    val id: Long,
    val fileList: List<String>,
    val createdAt: Long,
    val expiresAt: Long
)
