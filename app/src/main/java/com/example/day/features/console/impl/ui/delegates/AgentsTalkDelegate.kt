package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import com.example.day.features.console.impl.domain.agents.AgMessageHandler
import javax.inject.Inject

/** Делегат, отправляющий текст пользователя из чата агентам */
internal class AgentsTalkDelegate @Inject constructor(
    private val addChatMessageUseCase: AddChatMessageUseCase,
    private val agMessageHandler: AgMessageHandler
) : TalkDelegate {

    override suspend fun tryAddUserMessage(
        chat: Chat,
        inputText: String,
        onSuccess: () -> Unit
    ) {
        // добавить сообщение пользователя в чат
        addChatMessageUseCase.invoke(
            chatId = chat.id,
            timestamp = System.currentTimeMillis(),
            userType = UserType.User,
            text = inputText,
            status = ChatMessageStatus.Viewed,
            type = ChatMessage.Type.User
        )

        // обработчик сообщения пользователя агентами
        agMessageHandler.handleUserMessage(
            userMessage = inputText,
            chat = chat
        )
    }

    override suspend fun tryHandleAction(
        chat: Chat,
        messageId: Long,
        action: String
    ) {
        // Not supported for agents
    }
}
