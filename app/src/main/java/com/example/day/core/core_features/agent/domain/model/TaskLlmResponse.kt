package com.example.day.core.core_features.agent.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON response structure from LLM in task workflow.
 *
 * @property thought Internal reasoning of the agent
 * @property replyToUser Text shown to user in chat
 * @property memoryUpdates Key-value pairs to save to LTM
 * @property workflowTransition Next state name or null to stay in current state
 */
@Serializable
data class TaskLlmResponse(
    @SerialName("thought")
    val thought: String = "",

    @SerialName("reply_to_user")
    val replyToUser: String,

    @SerialName("memory_updates")
    val memoryUpdates: Map<String, String> = emptyMap(),

    @SerialName("workflow_transition")
    val workflowTransition: String? = null
)
