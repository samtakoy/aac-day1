package com.example.day.features.console.impl.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.day.core.core_features.chat.domain.model.ChatMessageStatus
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.core_features.chat.domain.model.UserType
import com.example.day.core.core_features.chat.domain.usecase.ClearChatNotViewedMessageUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatByIdAsFlowUseCase
import com.example.day.core.core_features.chat.domain.usecase.GetChatMessagesAsFlowUseCase
import com.example.day.core.core_features.chat.domain.usecase.UpdateChatSettingsUseCase
import com.example.day.core.ui.uikit.chat.bar.model.ChatBarUiModel
import com.example.day.core.ui.uikit.chat.bar.model.ChatSendButtonType
import com.example.day.core.ui.uikit.chat.list.model.ChatListUiModel
import com.example.day.core.ui.uikit.chat.list.model.ChatMessageUiModel
import com.example.day.core.ui.uikit.chat.list.model.UiMessageStatus
import com.example.day.features.console.impl.ui.components.ChatSettingsUiModel
import com.example.day.features.console.impl.ui.delegates.AgentsTalkDelegate
import com.example.day.features.console.impl.ui.delegates.LlmTalkDelegate
import com.example.day.features.console.impl.ui.delegates.TalkDelegate
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
    private val getMessagesUseCase: GetChatMessagesAsFlowUseCase,
    private val clearUnviewedUseCase: ClearChatNotViewedMessageUseCase,
    private val talkDelegate: TalkDelegate,
    private val getChatByIdAsFlowUseCase: GetChatByIdAsFlowUseCase,
    private val updateChatSettingsUseCase: UpdateChatSettingsUseCase,
    // TODO
    // private val savedStateHandle: SavedStateHandle,
    private val chatId: Long
) : ViewModel(), ConsoleViewModel {

    private val _state = MutableStateFlow(
        ConsoleViewModel.State(
            chatList = ChatListUiModel(emptyList<ChatMessageUiModel>().toPersistentList()),
            chatBar = ChatBarUiModel(
                inputInitialValue = "",
                buttonType = ChatSendButtonType.ArrowDisabled
            ),
            settings = null
        )
    )

    // Chat settings loaded from database
    private var chatSettings: ChatSettings? = null

    init {
        // Subscribe to chat data to get settings
        getChatByIdAsFlowUseCase(chatId)
            .onEach { chat ->
                chat?.settings?.let { settings ->
                    chatSettings = settings
                }
            }
            .launchIn(viewModelScope)

        // Subscribe to messages
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

    override fun getStateAsFlow(): StateFlow<ConsoleViewModel.State> = _state

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
            ConsoleViewModel.Event.OpenSettingsClick -> {
                chatSettings?.let { settings ->
                    _state.update { it.copy(settings = ChatSettingsUiModel("Настройки", settings)) }
                }
            }

            ConsoleViewModel.Event.SettingsCancelClick -> {
                _state.update { it.copy(settings = null) }
            }
            is ConsoleViewModel.Event.SettingsSubmitClick -> {
                chatSettings = event.result
                _state.update { it.copy(settings = null) }
                // Save settings to database
                viewModelScope.launch {
                    updateChatSettingsUseCase(event.result)
                }
            }
        }
    }

    private fun sendRequest(inputText: String) {
        val settings = chatSettings ?: return
        changeSendBar("", ChatSendButtonType.ArrowDisabled)
        launchCatching(
            onError = { error ->
                restoreSendButton()
                clearUnviewedUseCase.invoke(chatId)
            }
        ) {
            clearUnviewedUseCase.invoke(chatId)
            // делегат отправки сообщения куда-то
            talkDelegate.tryAddUserMessage(chatId, inputText, settings) {
                restoreSendButton()
            }
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
        private val getMessagesUseCase: GetChatMessagesAsFlowUseCase,
        private val clearUnviewedUseCase: ClearChatNotViewedMessageUseCase,
        private val talkDelegate: LlmTalkDelegate,
        private val getChatByIdAsFlowUseCase: GetChatByIdAsFlowUseCase,
        private val updateChatSettingsUseCase: UpdateChatSettingsUseCase,
    ): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            // TODO
            // val savedStateHandle = extras.createSavedStateHandle()
            val id = extras[CHAT_ID_KEY] ?: error("ID not found in extras")
            return ConsoleViewModelImpl(
                getMessagesUseCase,
                clearUnviewedUseCase,
                talkDelegate,
                getChatByIdAsFlowUseCase,
                updateChatSettingsUseCase,
                id
            ) as T
        }
    }

    class AgentFactory @Inject constructor(
        private val getMessagesUseCase: GetChatMessagesAsFlowUseCase,
        private val clearUnviewedUseCase: ClearChatNotViewedMessageUseCase,
        private val talkDelegate: AgentsTalkDelegate,
        private val getChatByIdAsFlowUseCase: GetChatByIdAsFlowUseCase,
        private val updateChatSettingsUseCase: UpdateChatSettingsUseCase,
    ): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            // TODO
            // val savedStateHandle = extras.createSavedStateHandle()
            val chatId = extras[CHAT_ID_KEY] ?: error("ID not found in extras")
            return ConsoleViewModelImpl(
                getMessagesUseCase,
                clearUnviewedUseCase,
                talkDelegate,
                getChatByIdAsFlowUseCase,
                updateChatSettingsUseCase,
                chatId = chatId
            ) as T
        }
    }

    companion object {
        val CHAT_ID_KEY = object : CreationExtras.Key<Long> {}
    }
}