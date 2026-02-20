package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.features.console.impl.domain.model.ChatSettings
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.ChangeMessageStatusUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatMessagesWithStatusUseCase
import com.example.day.features.console.impl.domain.LlmRequestUseCase
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
        requestUseCase.exec(inputText, history, chatSettings)
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
}