package com.example.day.core.core_features.agent.domain.workers.task.states_config

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskStateMessage(
    @SerialName("role")
    val role: Role,
    @SerialName("content")
    val content: String
) {
    enum class Role { SYSTEM, USER, ASSISTANT }
}

fun TaskStateMessage.toContextMessage() = AContextMessage(
    role = role.toContextRole(),
    content = content
)

fun TaskStateMessage.Role.toContextRole(): AContextMessage.Role = when (this) {
    TaskStateMessage.Role.SYSTEM -> AContextMessage.Role.SYSTEM
    TaskStateMessage.Role.USER -> AContextMessage.Role.USER
    TaskStateMessage.Role.ASSISTANT -> AContextMessage.Role.ASSISTANT
}

fun AContextMessage.toTaskMessage() = TaskStateMessage(
    role = role.toTaskRole(),
    content = content
)

fun AContextMessage.Role.toTaskRole(): TaskStateMessage.Role = when (this) {
    AContextMessage.Role.SYSTEM -> TaskStateMessage.Role.SYSTEM
    AContextMessage.Role.USER -> TaskStateMessage.Role.USER
    AContextMessage.Role.ASSISTANT -> TaskStateMessage.Role.ASSISTANT
    AContextMessage.Role.TOOL -> TaskStateMessage.Role.ASSISTANT // TOOL не поддерживается в TaskStateMessage
}