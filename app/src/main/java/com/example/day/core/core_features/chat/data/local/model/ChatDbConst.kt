package com.example.day.core.core_features.chat.data.local.model

/**
 * Database constants for Chat feature.
 * Centralizes all magic numbers used in the database layer.
 */
internal object ChatDbConst {
    // User type constants (stored as Int in database)
    const val BOT_TYPE = 1
    const val USER_TYPE = 2

    // Chat message status constants (stored as Int in database)
    const val MESSAGE_STATUS_SENDING = 1
    const val MESSAGE_STATUS_DELIVERED = 2
    const val MESSAGE_STATUS_VIEWED = 3

    // Default user names
    const val DEFAULT_USER_NAME = "User"
    const val DEFAULT_BOT_NAME = "LLM"
}
