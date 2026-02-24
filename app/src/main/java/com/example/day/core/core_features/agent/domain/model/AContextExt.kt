package com.example.day.core.core_features.agent.domain.model

import com.example.day.core.core_features.llm.domain.model.ModelRequest
import kotlinx.collections.immutable.toImmutableList

/**
 * Extension функции для работы с AContext
 */

/**
 * Добавить сообщение пользователя в контекст
 */
fun AContext.addUserMessage(content: String): AContext {
    val newOrderNumber = if (messages.isEmpty()) 1L else messages.last().orderNumber + 1
    val newMessage = AContextMessage(
        role = Role.USER,
        content = content,
        orderNumber = newOrderNumber
    )
    return copy(
        messages = (messages + newMessage).toImmutableList()
    )
}

/**
 * Добавить сообщение ассистента в контекст
 */
fun AContext.addAssistantMessage(content: String): AContext {
    val newOrderNumber = if (messages.isEmpty()) 1L else messages.last().orderNumber + 1
    val newMessage = AContextMessage(
        role = Role.ASSISTANT,
        content = content,
        orderNumber = newOrderNumber
    )
    return copy(
        messages = (messages + newMessage).toImmutableList()
    )
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
