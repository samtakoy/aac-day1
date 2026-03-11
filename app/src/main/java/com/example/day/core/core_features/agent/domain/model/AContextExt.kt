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
fun PersistentList<AContextMessage>.addUserMessage(prompt: AContextMessage): PersistentList<AContextMessage> {
    // Пустое сообщение может быть когда надо просто подергать llm
    if (prompt.content.isNullOrBlank()) return this
    return this.mutate {
        it.add(prompt)
    }
}

/**
 * Добавить сообщение ассистента в контекст
 */
fun PersistentList<AContextMessage>.addAssistantMessage(content: String): PersistentList<AContextMessage> {
    val newMessage = AContextMessage(
        role = AContextMessage.Role.ASSISTANT,
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
        AContextMessage.Role.SYSTEM -> ModelRequest.Role.System
        AContextMessage.Role.USER -> ModelRequest.Role.User
        AContextMessage.Role.ASSISTANT -> ModelRequest.Role.Assistant
        AContextMessage.Role.TOOL -> ModelRequest.Role.Tool
    }
    return ModelRequest.Message(
        role = modelRole,
        content = content,
        toolCallId = toolCallId,
        toolCalls = toolCalls?.map { ref ->
            ModelRequest.ToolCall(
                id = ref.id,
                type = ref.type,
                function = ModelRequest.FunctionCall(
                    name = ref.functionName,
                    arguments = ref.arguments
                )
            )
        }
    )
}

/**
 * Конвертировать список AContextMessage в список ModelRequest.Message
 */
fun List<AContextMessage>.toModelRequestMessages(): List<ModelRequest.Message> {
    return this.map { it.toModelRequestMessage() }
}
