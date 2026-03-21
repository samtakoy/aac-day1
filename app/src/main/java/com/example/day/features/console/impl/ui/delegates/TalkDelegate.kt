package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.chat.domain.model.Chat
import kotlinx.coroutines.flow.SharedFlow

interface TalkDelegate {
    suspend fun tryAddUserMessage(
        chat: Chat,
        inputText: String,
        onSuccess: () -> Unit
    )

    suspend fun tryHandleAction(
        chat: Chat,
        messageId: Long,
        action: String
    )

    suspend fun tryHandleConfirmation(
        chat: Chat,
        runId: String,
        confirmationId: String,
        approved: Boolean
    ) {
        // Optional. Not all delegates support resumable tool-calling.
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getPlannerEvents(): SharedFlow<T>? = null
}
