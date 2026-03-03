package com.example.day.core.core_features.chat.domain.model

/**
 * Domain model representing a completed stage artifact.
 * Stores the final result/outcome from a stage chat.
 *
 * @property id Unique identifier
 * @property chatId The stage chat that produced this artifact
 * @property stageTitle Title of the stage
 * @property content The artifact content (summary, code, solution, etc.)
 * @property createdAt Timestamp when artifact was created
 */
data class Artifact(
    val id: Long = 0,
    val chatId: Long,
    val stageTitle: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
