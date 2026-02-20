package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.features.console.impl.domain.model.ChatSettings
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.ChangeMessageStatusUseCase
import com.example.day.features.console.impl.domain.agents.AgMessageHandler
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/** Делегат, отправляющий текст пользователя из чата агентам */
internal class AgentsTalkDelegate @Inject constructor(
    private val addChatMessageUseCase: AddChatMessageUseCase,
    private val changeMessageUseCase: ChangeMessageStatusUseCase,
    private val agMessageHandler: AgMessageHandler
) : TalkDelegate {
    override suspend fun tryAddUserMessage(
        chatId: Long,
        inputText: String,
        chatSettings: ChatSettings,
        onSuccess: () -> Unit
    ) {
        // добавить сообщение
        val messageId = addChatMessageUseCase.invoke(
            chatId,
            System.currentTimeMillis(),
            UserType.User,
            inputText,
            ChatMessageStatus.Sending
        )
        coroutineScope {
            agMessageHandler
                .handleUserMessage(inputText, chatSettings)
                .onStart {
                    // сообщение пользователя подтверждаем как просмотренное
                    changeMessageUseCase(messageId, ChatMessageStatus.Viewed)
                }
                .onEach { result ->
                    // добавляем в чат ответ бота
                    addChatMessageUseCase.invoke(
                        chatId,
                        System.currentTimeMillis(),
                        UserType.Bot,
                        result,
                        ChatMessageStatus.Viewed
                    )
                }
                .launchIn(this)
        }
    }
}