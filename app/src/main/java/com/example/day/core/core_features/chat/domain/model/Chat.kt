package com.example.day.core.core_features.chat.domain.model

/**
 * Domain model for Chat.
 * Includes planner-related fields for hierarchical chat structure:
 * - parentId: ID of parent chat (for stage chats within planner)
 * - workingSummary: Dynamic context/memory for this chat
 * - isPlannerMain: Whether this is the main chat in a planner group
 */
data class Chat(
    val id: Long,
    val title: String,
    val chatGroup: ChatGroup,
    val settings: ChatSettings,
    val parentId: Long? = null,
    val workingSummary: String? = null,
    val isPlannerMain: Boolean = false
) {
    /**
     * True if this is a stage chat within a planner (has a parent chat)
     */
    val isStageChat: Boolean
        get() = parentId != null
}
