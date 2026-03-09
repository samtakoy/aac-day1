package com.example.day.core.core_features.state_machine.domain

import com.example.day.core.core_features.chat.domain.model.ChatMessage

/**
 * Result type for state handler operations.
 *
 * @param messages сообщения для чата
 */
class HandlerResult(
    val messages: List<Message>,
    val llmRequest: LlmRequest? = null,
    val errorMessage: String? = null
) {
    /**
     * @param userPrompt может быть - просто чтобы получить ответ от Llm
     * */
    data class LlmRequest(
        val userPrompt: String? = null
    )

    // TODO refactor
    data class Message(
        val message: String,
        val isInfo: Boolean = false,
        val isTitle: Boolean = false,
        val buttons: List<ChatMessage.Button> = emptyList()
    )

    companion object {
        fun error(errorMessage: String) = HandlerResult(emptyList(), errorMessage = errorMessage)
    }
}

