package com.example.day.core.core_features.agent.domain.workers.task.states_config

import com.example.day.core.core_features.agent.domain.model.AContextMessage
import com.example.day.core.core_features.agent.domain.model.Role
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

fun TaskStateMessage.Role.toContextRole(): Role = when (this) {
    TaskStateMessage.Role.SYSTEM -> Role.SYSTEM
    TaskStateMessage.Role.USER -> Role.USER
    TaskStateMessage.Role.ASSISTANT -> Role.ASSISTANT
}

fun AContextMessage.toTaskMessage() = TaskStateMessage(
    role = role.toTaskRole(),
    content = content
)

fun Role.toTaskRole(): TaskStateMessage.Role = when (this) {
    Role.SYSTEM -> TaskStateMessage.Role.SYSTEM
    Role.USER -> TaskStateMessage.Role.USER
    Role.ASSISTANT -> TaskStateMessage.Role.ASSISTANT
}