package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.agent.domain.workers.base.WorkerEvent
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.model.ChatMessage
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import com.example.day.features.console.impl.domain.agents.AgMessageHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

/** Delegate that forwards user messages from chat to agent workers. */
internal class AgentsTalkDelegate @Inject constructor(
    private val addChatMessageUseCase: AddChatMessageUseCase,
    private val agMessageHandler: AgMessageHandler
) : TalkDelegate {

    private val _plannerEventsFlow = MutableSharedFlow<PlannerUiEvent>()
    private val plannerEventsFlow: SharedFlow<PlannerUiEvent> = _plannerEventsFlow.asSharedFlow()

    override suspend fun tryAddUserMessage(
        chat: Chat,
        inputText: String,
        onSuccess: () -> Unit
    ) {
        addChatMessageUseCase.invoke(
            chatId = chat.id,
            timestamp = System.currentTimeMillis(),
            userType = UserType.User,
            text = inputText,
            status = ChatMessageStatus.Viewed,
            type = ChatMessage.Type.User
        )

        onSuccess.invoke()

        agMessageHandler.handleUserMessage(
            userMessage = inputText,
            chat = chat,
            onEvent = ::handleWorkerEvent
        )
    }

    override suspend fun tryHandleAction(
        chat: Chat,
        messageId: Long,
        action: String
    ) {
        // Not supported for agents.
    }

    override suspend fun tryHandleConfirmation(
        chat: Chat,
        runId: String,
        confirmationId: String,
        approved: Boolean
    ) {
        agMessageHandler.handleConfirmation(
            chat = chat,
            runId = runId,
            confirmationId = confirmationId,
            approved = approved,
            onEvent = ::handleWorkerEvent
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getPlannerEvents(): SharedFlow<T>? {
        return plannerEventsFlow as? SharedFlow<T>
    }

    private suspend fun handleWorkerEvent(event: WorkerEvent) {
        when (event) {
            is WorkerEvent.UserConfirmation.Requested -> {
                _plannerEventsFlow.emit(
                    PlannerUiEvent.UserConfirmation(
                        id = event.confirmationId,
                        runId = event.runId,
                        title = event.title,
                        message = event.message,
                        actionLabel = event.actionLabel
                    )
                )
            }
            else -> Unit
        }
    }
}
