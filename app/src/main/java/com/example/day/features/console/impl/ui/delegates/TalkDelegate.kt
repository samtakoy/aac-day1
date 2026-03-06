package com.example.day.features.console.impl.ui.delegates

import com.example.day.core.core_features.chat.domain.model.Chat
import kotlinx.coroutines.flow.SharedFlow

/**
 * Base interface for all TalkDelegates.
 * Provides optional plannerEvents for PLANNER-type delegates.
 */
interface TalkDelegate {
    suspend fun tryAddUserMessage(
        chat: Chat,
        inputText: String,
        onSuccess: () -> Unit
    )

    /**
     * Handle button click action from chat message.
     */
    suspend fun tryHandleAction(
        chat: Chat,
        messageId: Long,
        action: String
    )

    /**
     * Optional events stream for PLANNER-type delegates.
     * Returns null for non-PLANNER delegates.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getPlannerEvents(): SharedFlow<T>? = null
}