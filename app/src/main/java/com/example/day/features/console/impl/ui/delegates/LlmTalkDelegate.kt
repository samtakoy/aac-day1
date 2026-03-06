package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.ChatMessage.Type
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.ChangeMessageStatusUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatMessagesWithStatusUseCase
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import com.example.day.core.core_features.llm.domain.model.ModelRequest
import com.example.day.core.core_features.llm.domain.model.getContent
import javax.inject.Inject

/** Ведет простой диалог с Llm.
 * В каждый запрос к Llm добавляет историю сообщений которую читает прямо из чата (все подряд)
 * */
internal class LlmTalkDelegate @Inject constructor(
    private val requestUseCase: LlmRequestUseCase,
    private val getMessagesWithStatusUseCase: GetChatMessagesWithStatusUseCase,
    private val addChatMessageUseCase: AddChatMessageUseCase,
    private val changeMessageUseCase: ChangeMessageStatusUseCase,
) : TalkDelegate {

    override suspend fun tryAddUserMessage(
        chat: Chat,
        inputText: String,
        onSuccess: () -> Unit
    ) {
        val messageId = addChatMessageUseCase.invoke(
            chat.id,
            System.currentTimeMillis(),
            UserType.User,
            inputText,
            ChatMessageStatus.Sending,
            Type.User
        )
        val history = getMessagesWithStatusUseCase(chat.id, ChatMessageStatus.Viewed)
        llmRequest(inputText, history, chat.settings)
            .onSuccess { llmResult ->
                changeMessageUseCase(messageId, ChatMessageStatus.Viewed)
                addChatMessageUseCase.invoke(
                    chat.id,
                    System.currentTimeMillis(),
                    UserType.Bot,
                    llmResult.getContent(),
                    ChatMessageStatus.Viewed,
                    Type.Bot
                )
                onSuccess()
            }
            .getOrThrow()
    }

    private suspend fun llmRequest(
        promptText: String,
        history: List<ChatMessage>,
        chatSettings: com.example.day.core.core_features.chat.domain.model.ChatSettings
    ) = requestUseCase.exec(
        modelSettings = chatSettings.model,
        systemPrompt = chatSettings.systemPromt,
        messages = history.map { chatMessage ->
            chatMessage.mapToRequestMessage()
        },
        promptText = promptText,
    )

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

    override suspend fun tryHandleAction(
        chat: Chat,
        messageId: Long,
        action: String
    ) {
        // Not supported for LLM direct chat
    }
}
