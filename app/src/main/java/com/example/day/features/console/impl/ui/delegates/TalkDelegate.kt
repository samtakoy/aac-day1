package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.chat.domain.model.Chat

interface TalkDelegate {
    suspend fun tryAddUserMessage(
        chatId: Long,
        inputText: String,
        chat: Chat,
        onSuccess: () -> Unit
    )
}