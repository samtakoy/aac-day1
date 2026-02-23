package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.chat.domain.model.Chat

interface TalkDelegate {
    suspend fun tryAddUserMessage(
        chat: Chat,
        inputText: String,
        onSuccess: () -> Unit
    )
}