package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.ChangeMessageStatusUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatMessagesWithStatusUseCase
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import javax.inject.Inject

/** Ведет простой диалог с Llm */
internal class LlmTalkDelegate @Inject constructor(
    private val requestUseCase: LlmRequestUseCase,
    private val getMessagesWithStatusUseCase: GetChatMessagesWithStatusUseCase,
    private val addChatMessageUseCase: AddChatMessageUseCase,
    private val changeMessageUseCase: ChangeMessageStatusUseCase,
) : TalkDelegate {

    override suspend fun tryAddUserMessage(
        chatId: Long,
        inputText: String,
        chatSettings: ChatSettings,
        onSuccess: () -> Unit
    ) {
        val messageId = addChatMessageUseCase.invoke(
            chatId,
            System.currentTimeMillis(),
            UserType.User,
            inputText,
            ChatMessageStatus.Sending
        )
        val history = getMessagesWithStatusUseCase(chatId, ChatMessageStatus.Viewed)
        llmRequest(inputText, history, chatSettings)
            .onSuccess { result ->
                changeMessageUseCase(messageId, ChatMessageStatus.Viewed)
                addChatMessageUseCase.invoke(
                    chatId,
                    System.currentTimeMillis(),
                    UserType.Bot,
                    result,
                    ChatMessageStatus.Viewed
                )
                onSuccess()
            }
            .getOrThrow()
    }

    private suspend fun llmRequest(
        promptText: String,
        history: List<ChatMessage>,
        chatSettings: ChatSettings
    ): Result<String> {
        return requestUseCase.exec(
            modelSettings = chatSettings.model,
            systemPrompt = chatSettings.systemPromt,
            messages = history.map { chatMessage ->
                chatMessage.mapToRequestMessage()
            },
            promptText = promptText,
        )
    }

    private fun ChatMessage.mapToRequestMessage(): ModelRequest.Message = ModelRequest.Message(
        role = user.type.mapToRequestRole(),
        content = text,
        thinking = null,
        cachePrompt = false
    )

    private fun UserType.mapToRequestRole(): ModelRequest.Role =
        if (this == UserType.User) {
            ModelRequest.Role.User
        } else {
            ModelRequest.Role.Assistant
        }
}