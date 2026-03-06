package com.example.day.core.core_features.agent.domain.model

import com.example.day.core.core_features.agent.domain.workers.task.TaskMemKeys
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON response structure from LLM in task workflow.
 *
 * @property replyToUser Text shown to user in chat
 * @property memoryUpdates Key-value pairs to save to LTM
 * @property nextState Task status from LLM
 */
@Serializable
data class TaskLlmResponse(
    @SerialName(TaskMemKeys.REPLY_TO_USER)
    val replyToUser: String,

    @SerialName(TaskMemKeys.MEM_UPDATES)
    val memoryUpdates: Map<String, String?> = emptyMap(),

    @SerialName(TaskMemKeys.NEXT_STATE)
    val nextState: String? = null,

    @SerialName(TaskMemKeys.USER_APPROVE)
    val userApprove: String? = null
)
