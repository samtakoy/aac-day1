package com.example.day.core.core_features.agent.domain.model

import com.example.day.core.core_features.llm.domain.model.ModelRequest
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toImmutableList

/**
 * Extension функции для работы с AContext
 */

/**
 * Добавить сообщение пользователя в контекст
 */
fun PersistentList<AContextMessage>.addUserMessage(content: String?): PersistentList<AContextMessage> {
    // Пустое сообщение может быть когда надо просто подергать llm
    if (content.isNullOrBlank()) return this
    val newMessage = AContextMessage(
        role = Role.USER,
        content = content
    )
    return this.mutate {
        it.add(newMessage)
    }
}

/**
 * Добавить сообщение ассистента в контекст
 */
fun PersistentList<AContextMessage>.addAssistantMessage(content: String): PersistentList<AContextMessage> {
    val newMessage = AContextMessage(
        role = Role.ASSISTANT,
        content = content,
    )
    return this.mutate {
        it.add(newMessage)
    }
}

/**
 * Конвертировать AContextMessage в ModelRequest.Message
 */
fun AContextMessage.toModelRequestMessage(): ModelRequest.Message {
    val modelRole = when (role) {
        Role.SYSTEM -> ModelRequest.Role.System
        Role.USER -> ModelRequest.Role.User
        Role.ASSISTANT -> ModelRequest.Role.Assistant
    }
    return ModelRequest.Message(
        role = modelRole,
        content = content
    )
}

/**
 * Конвертировать список AContextMessage в список ModelRequest.Message
 */
fun List<AContextMessage>.toModelRequestMessages(): List<ModelRequest.Message> {
    return this.map { it.toModelRequestMessage() }
}
