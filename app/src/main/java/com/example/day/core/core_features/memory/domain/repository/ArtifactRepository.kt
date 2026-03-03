package com.example.day.core.core_features.memory.domain.repository

import com.example.day.core.core_features.memory.domain.model.Artifact
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for artifact operations.
 * Manages completed stage results/outcomes.
 */
interface ArtifactRepository {

    /**
     * Save an artifact from a completed stage.
     *
     * @param chatId The stage chat ID that produced this artifact
     * @param stageTitle Title of the stage
     * @param content The artifact content (summary, solution, code, etc.)
     * @return ID of the saved artifact
     */
    suspend fun saveArtifact(chatId: Long, stageTitle: String, content: String): Long

    /**
     * Get all artifacts for a specific stage chat.
     */
    fun getArtifactsForChat(chatId: Long): Flow<List<Artifact>>

    /**
     * Get all artifacts for child chats of a parent chat.
     * Used to collect all stage results from a planner.
     *
     * @param parentChatId The main planner chat ID
     */
    fun getArtifactsForParent(parentChatId: Long): Flow<List<Artifact>>

    /**
     * Delete all artifacts for a specific chat.
     * Called when a stage chat is deleted.
     */
    suspend fun deleteArtifactsForChat(chatId: Long)
}
