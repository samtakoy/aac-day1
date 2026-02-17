package com.example.day.features.console.impl.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.usecase.AddChatMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.ChangeMessageStatusUseCase
import com.example.day.core.core_features.chat.domain.usecase.ClearChatNotViewedMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatMessagesAsFlowUseCase
import com.example.day.core.ui.uikit.chat.bar.model.ChatBarUiModel
import com.example.day.core.ui.uikit.chat.bar.model.ChatSendButtonType
import com.example.day.core.ui.uikit.chat.list.model.ChatListUiModel
import com.example.day.core.ui.uikit.chat.list.model.ChatMessageUiModel
import com.example.day.core.ui.uikit.chat.list.model.UiMessageStatus
import com.example.day.features.console.impl.domain.LlmRequestUseCase
import com.example.day.features.console.impl.ui.viewmodel.ConsoleViewModel.State
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ConsoleViewModelImpl(
    private val requestUseCase: LlmRequestUseCase,
    private val getMessagesUseCase: GetChatMessagesAsFlowUseCase,
    private val clearUnviewedUseCase: ClearChatNotViewedMessageUseCase,
    private val addChatMessageUseCase: AddChatMessageUseCase,
    private val changeMessageUseCase: ChangeMessageStatusUseCase,
    // TODO
    // private val savedStateHandle: SavedStateHandle,
    private val chatId: Long
) : ViewModel(), ConsoleViewModel {

    private val _state = MutableStateFlow(
        State(
            chatList = ChatListUiModel(emptyList<ChatMessageUiModel>().toPersistentList()),
            chatBar = ChatBarUiModel(inputInitialValue = "", buttonType = ChatSendButtonType.ArrowDisabled)
        )
    )

    init {
        getMessagesUseCase(chatId)
            .onEach { messages ->
                _state.update { state ->
                    state.copy(
                        chatList = state.chatList.copy(
                            messages = messages.map { msg ->
                                ChatMessageUiModel(
                                    id = msg.id,
                                    text = msg.text,
                                    isUser = msg.user.type == UserType.User,
                                    status = when (msg.status) {
                                        ChatMessageStatus.Sending -> UiMessageStatus.Sending
                                        ChatMessageStatus.Delivered -> UiMessageStatus.Delivered
                                        ChatMessageStatus.Viewed -> UiMessageStatus.Viewed
                                    }
                                )
                            }.toPersistentList()
                        )
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun getStateAsFlow(): StateFlow<State> = _state

    private fun onInputChanged(text: String) {
        _state.update { state ->
            val prevBar = state.chatBar
            state.copy(
                chatBar = prevBar.copy(
                    inputInitialValue = text,
                    buttonType = resolveButtonType(prevBar, text)
                )
            )
        }
    }

    private fun resolveButtonType(
        prevBar: ChatBarUiModel,
        newText: String
    ): ChatSendButtonType {
        return when {
            prevBar.buttonType == ChatSendButtonType.Loading -> prevBar.buttonType
            newText.isNotEmpty() -> ChatSendButtonType.Arrow
            else -> ChatSendButtonType.ArrowDisabled
        }
    }

    override fun onEvent(event: ConsoleViewModel.Event) {
        when (event) {
            is ConsoleViewModel.Event.InputChanged -> onInputChanged(event.text)
            is ConsoleViewModel.Event.SubmitButtonClick -> {
                sendRequest(_state.value.chatBar.inputInitialValue)
            }
        }
    }

    private fun sendRequest(inputText: String) {
        changeSendBar("", ChatSendButtonType.ArrowDisabled)
        launchCatching(
            onError = { error ->
                restoreSendButton()
                clearUnviewedUseCase.invoke(chatId)
            }
        ) {
            clearUnviewedUseCase.invoke(chatId)
            val messageId = addChatMessageUseCase.invoke(
                chatId,
                System.currentTimeMillis(),
                UserType.User,
                inputText,
                ChatMessageStatus.Sending
            )
            val result = requestUseCase.exec(inputText)
                .onSuccess { result ->
                    changeMessageUseCase(messageId, ChatMessageStatus.Viewed)
                    addChatMessageUseCase.invoke(
                        chatId,
                        System.currentTimeMillis(),
                        UserType.Bot,
                        result,
                        ChatMessageStatus.Viewed
                    )
                    restoreSendButton()
                }
                .onFailure { result ->
                    restoreSendButton()
                    clearUnviewedUseCase.invoke(chatId)
                }
                .getOrNull()
        }
    }

    private fun changeSendBar(text: String, buttonType: ChatSendButtonType) {
        _state.update { state ->
            state.copy(chatBar = state.chatBar.copy(inputInitialValue = text, buttonType = buttonType))
        }
    }

    private fun restoreSendButton() {
        _state.update { state ->
            state.copy(
                chatBar = state.chatBar.copy(
                    buttonType = if (state.chatBar.inputInitialValue.isNotEmpty()) ChatSendButtonType.Arrow else ChatSendButtonType.ArrowDisabled
                )
            )
        }
    }

    /** TODO дублирование */
    private fun launchCatching(
        onError: (suspend (Throwable) -> Unit)? = null,
        onFinally: (suspend () -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                onError?.invoke(e)
            } finally {
                onFinally?.invoke()
            }
        }
    }

    class Factory @Inject constructor(
        private val requestUseCase: LlmRequestUseCase,
        private val getMessagesUseCase: GetChatMessagesAsFlowUseCase,
        private val clearUnviewedUseCase: ClearChatNotViewedMessageUseCase,
        private val addChatMessageUseCase: AddChatMessageUseCase,
        private val changeMessageUseCase: ChangeMessageStatusUseCase,
    ): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            // TODO
            // val savedStateHandle = extras.createSavedStateHandle()
            val id = extras[ID_KEY] ?: error("ID not found in extras")
            return ConsoleViewModelImpl(
                requestUseCase,
                getMessagesUseCase,
                clearUnviewedUseCase,
                addChatMessageUseCase,
                changeMessageUseCase,
                id
            ) as T
        }
    }

    companion object {
        val ID_KEY = object : CreationExtras.Key<Long> {}
    }
}