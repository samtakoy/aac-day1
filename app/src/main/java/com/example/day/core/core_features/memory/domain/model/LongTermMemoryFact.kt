package com.example.day.core.core_features.memory.domain.model

/**
 * Domain model representing a user fact stored in long-term memory.
 * Used to personalize agent responses across chat sessions.
 * Memory is isolated per LTMGroup, which can be linked to ChatGroup, UserProfile, etc.
 *
 * @property memoryKey Unique identifier for the fact (e.g., "primary_language", "experience_level")
 * @property ltmGroupId LTM Group ID for memory isolation
 * @property category Category
 * @property fact The actual fact text (e.g., "Senior Kotlin Developer")
 * @property updatedAt Timestamp of last update
 */
data class LongTermMemoryFact(
    val memoryKey: String,
    val ltmGroupId: Long,
    val category: String,
    val fact: String,
    val updatedAt: Long = System.currentTimeMillis()
)
