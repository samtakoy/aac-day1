package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.ChangeMessageStatusUseCase
import com.example.day.core.core_features.chat.domain.usecase.CreateChatUseCase
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
    private val createChatUseCase: CreateChatUseCase,
    private val agMessageHandler: AgMessageHandler
) : TalkDelegate {

    override suspend fun tryAddUserMessage(
        chatId: Long,
        inputText: String,
        chat: Chat,
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
        
        // Получаем groupId из чата
        val groupId = chat.chatGroup.id
        
        // Функция создания нового чата - принимает имя модели и возвращает Pair<chatId, chatName>
        val createChat: suspend (String) -> Pair<Long, String> = { modelName ->
            // Создаем новый чат с именем модели
            val chatTitle = modelName
            val chatId = createChatUseCase.invoke(chatTitle, groupId)
            Pair(chatId, chatTitle)
        }
        
        coroutineScope {
            agMessageHandler
                .handleUserMessage(inputText, chat.settings, createChat)
                .onStart {
                    // сообщение пользователя подтверждаем как просмотренное
                    changeMessageUseCase(messageId, ChatMessageStatus.Viewed)
                }
                .onEach { response ->
                    // добавляем в чат ответ бота
                    addChatMessageUseCase.invoke(
                        response.chatId,
                        System.currentTimeMillis(),
                        UserType.Bot,
                        response.message,
                        ChatMessageStatus.Viewed
                    )
                }
                .launchIn(this)
        }
    }
}
