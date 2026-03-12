package com.example.day.core.core_features.mcp.data.mapper

import com.example.day.core.core_features.mcp.data.local.entity.GitFileCacheEntity
import com.example.day.core.core_features.mcp.domain.model.GitFileCache
import kotlinx.serialization.json.Json

internal object GitFileCacheMapper {
    fun toDomain(entity: GitFileCacheEntity, json: Json): GitFileCache = GitFileCache(
        id = entity.id,
        fileList = json.decodeFromString(entity.fileListJson),
        createdAt = entity.createdAt,
        expiresAt = entity.expiresAt
    )

    fun toEntity(domain: GitFileCache, json: Json): GitFileCacheEntity = GitFileCacheEntity(
        id = domain.id,
        fileListJson = json.encodeToString(domain.fileList),
        createdAt = domain.createdAt,
        expiresAt = domain.expiresAt
    )
}
