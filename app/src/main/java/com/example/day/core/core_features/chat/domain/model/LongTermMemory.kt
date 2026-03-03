package com.example.day.core.core_features.chat.domain.model

/**
 * Domain model representing a user fact stored in long-term memory.
 * Used to personalize agent responses across chat sessions.
 * Memory is isolated per ChatGroup.
 *
 * @property memoryKey Unique identifier for the fact (e.g., "primary_language", "experience_level")
 * @property groupId ChatGroup ID for memory isolation
 * @property category Category of the fact: "skills", "preferences", "experience", "personal"
 * @property fact The actual fact text (e.g., "Senior Kotlin Developer")
 * @property updatedAt Timestamp of last update
 */
data class LongTermMemory(
    val memoryKey: String,
    val groupId: Long,
    val category: String,
    val fact: String,
    val updatedAt: Long = System.currentTimeMillis()
)
