package com.example.day.features.console.impl.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.example.day.core.core_features.chat.domain.model.ChatSettings
import com.example.day.core.ui.uikit.chat.bar.model.ChatBarUiModel
import com.example.day.core.ui.uikit.chat.list.model.ChatListUiModel
import com.example.day.features.console.impl.ui.components.ChatSettingsUiModel
import kotlinx.coroutines.flow.StateFlow

@Immutable
internal interface ConsoleViewModel {
    fun getStateAsFlow(): StateFlow<State>
    fun onEvent(event: Event)

    data class State(
        val chatList: ChatListUiModel,
        val chatBar: ChatBarUiModel,
        val settings: ChatSettingsUiModel?
    )

    sealed interface Event {
        object SubmitButtonClick : Event
        class InputChanged(val text: String) : Event
        object OpenSettingsClick : Event
        class SettingsSubmitClick(val chatTitle: String, val settings: ChatSettings) : Event
        object SettingsCancelClick : Event
    }
}