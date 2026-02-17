package com.example.day.core.ui.uikit.chat.bar.model

/**
 * Types for send button state
 */
enum class ChatSendButtonType {
    /**
     * Send button is disabled (no text to send)
     */
    ArrowDisabled,
    
    /**
     * Send button is enabled and ready to send
     */
    Arrow,
    
    /**
     * Send button is loading (showing spinner, not clickable)
     */
    Loading
}
