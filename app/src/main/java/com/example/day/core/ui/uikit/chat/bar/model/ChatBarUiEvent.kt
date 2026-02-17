package com.example.day.core.ui.uikit.chat.bar.model

/**
 * Sealed interface for chat input bar UI events
 */
sealed interface ChatBarUiEvent {
    /**
     * User changed text in input field
     */
    data class TextChange(val text: String) : ChatBarUiEvent
    
    /**
     * User clicked send button
     */
    data object SendClick : ChatBarUiEvent
}
