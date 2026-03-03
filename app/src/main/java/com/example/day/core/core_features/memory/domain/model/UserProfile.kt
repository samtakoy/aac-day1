package com.example.day.core.core_features.memory.domain.model

/**
 * Domain model representing a user profile.
 * Each profile has its own LTM group for isolated fact storage.
 *
 * @property id Unique profile identifier
 * @property title Profile name (e.g. "Work", "Personal")
 * @property ltmGroupId LTM group ID for accessing profile's long-term memory facts
 * @property textAvatar ASCII art avatar (30x30), null if not generated
 */
data class UserProfile(
    val id: Long,
    val title: String,
    // TODO используй
    val ltmGroupId: Long,
    val textAvatar: String?
)
