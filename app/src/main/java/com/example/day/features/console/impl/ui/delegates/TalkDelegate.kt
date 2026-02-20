package com.example.day.features.console.impl.ui.delegates

import com.example.day.features.console.impl.domain.model.ChatSettings

interface TalkDelegate {
    suspend fun tryAddUserMessage(
        chatId: Long,
        inputText: String,
        chatSettings: ChatSettings,
        onSuccess: () -> Unit
    )
}