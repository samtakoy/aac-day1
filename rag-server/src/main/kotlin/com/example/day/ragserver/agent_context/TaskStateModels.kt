package com.example.day.ragserver.agent_context

import kotlinx.serialization.Serializable

/**
 * Модели для endpoint POST /task-state/update.
 *
 * TODO: Этот endpoint — кандидат на переезд в отдельный AgentServer.
 *   Сейчас живёт в rag-server ради простоты (общий Ollama-клиент и инфраструктура).
 *   При выносе: вынести agent_context/ пакет целиком, добавить shared-module для LlmProvider.
 *   Точка входа в rag-server — только регистрация маршрута в RagServer.kt, без утечки логики.
 */

@Serializable
data class TaskStateUpdateRequest(
    /** JSON строка текущего TaskState. Передавать "{}" при первом обращении. */
    val currentState: String,
    /** Последние сообщения из истории (не более 4). */
    val lastMessages: List<MessageDto>,
)

@Serializable
data class MessageDto(
    /** "user" | "assistant" */
    val role: String,
    val content: String,
)

@Serializable
data class TaskStateUpdateResponse(
    /** Обновлённый TaskState в виде JSON строки. */
    val updatedState: String,
)
