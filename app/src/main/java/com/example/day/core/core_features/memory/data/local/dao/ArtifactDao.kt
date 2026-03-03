package com.example.day.core.core_features.memory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.day.core.core_features.memory.data.local.model.ProjectArtifactEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ArtifactDao {

    @Insert
    suspend fun insert(artifact: ProjectArtifactEntity): Long

    @Query("SELECT * FROM project_artifacts WHERE chat_id = :chatId ORDER BY created_at DESC")
    fun getByChatId(chatId: Long): Flow<List<ProjectArtifactEntity>>

    @Query("""
        SELECT * FROM project_artifacts
        WHERE chat_id IN (
            SELECT id FROM chats WHERE parent_id = :parentId
        )
        ORDER BY created_at ASC
    """)
    fun getByParentChatId(parentId: Long): Flow<List<ProjectArtifactEntity>>

    @Query("DELETE FROM project_artifacts WHERE chat_id = :chatId")
    suspend fun deleteByChatId(chatId: Long)
}
