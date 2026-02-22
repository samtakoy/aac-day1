package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.chat.domain.model.ChatSettings

interface TalkDelegate {
    suspend fun tryAddUserMessage(
        chatId: Long,
        inputText: String,
        chatSettings: ChatSettings,
        onSuccess: () -> Unit
    )
}