package com.example.day.core.core_features.agent.domain.workers.task

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Information about a task stage.
 * Used for planning and execution phases.
 */
@Serializable
data class StageInfo(
    @SerialName("name")
    val name: String,

    @SerialName("description")
    val description: String,

    @SerialName("expert")
    val expert: String
)