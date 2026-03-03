package com.example.day.core.core_features.memory.data.repository

import com.example.day.core.core_features.memory.data.local.dao.ArtifactDao
import com.example.day.core.core_features.memory.data.local.model.ProjectArtifactEntity
import com.example.day.core.core_features.memory.domain.model.Artifact
import com.example.day.core.core_features.memory.domain.repository.ArtifactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class ArtifactRepositoryImpl @Inject constructor(
    private val artifactDao: ArtifactDao
) : ArtifactRepository {

    override suspend fun saveArtifact(chatId: Long, stageTitle: String, content: String): Long {
        val entity = ProjectArtifactEntity(
            chatId = chatId,
            stageTitle = stageTitle,
            content = content
        )
        return artifactDao.insert(entity)
    }

    override fun getArtifactsForChat(chatId: Long): Flow<List<Artifact>> {
        return artifactDao.getByChatId(chatId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getArtifactsForParent(parentChatId: Long): Flow<List<Artifact>> {
        return artifactDao.getByParentChatId(parentChatId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun deleteArtifactsForChat(chatId: Long) {
        artifactDao.deleteByChatId(chatId)
    }

    private fun ProjectArtifactEntity.toDomain(): Artifact {
        return Artifact(
            id = id,
            chatId = chatId,
            stageTitle = stageTitle,
            content = content,
            createdAt = createdAt
        )
    }
}
