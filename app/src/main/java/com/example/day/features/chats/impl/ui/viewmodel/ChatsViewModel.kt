package com.example.day.features.chats.impl.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.example.day.core.core_features.chat.domain.model.ChatType
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.flow.StateFlow

@Immutable
internal interface ChatsViewModel {
    fun getStateAsFlow(): StateFlow<State>
    fun onEvent(event: Event)

    data class State(
        val chips: PersistentList<Chat>,
    ) {
        data class Chat(
            val id: Long,
            val chatType: ChatType,
            val title: String,
            val isSelected: Boolean
        )
    }

    sealed interface Event {
        // Пока чипсы не кликабельные
        // class ChatClick(val id: Long) : Event
        class ChatClick(val id: Long) : Event
        class SwipeTo(val id: Long) : Event
        object AddChatClick : Event
    }
}