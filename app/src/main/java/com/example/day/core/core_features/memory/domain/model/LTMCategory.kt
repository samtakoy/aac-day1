package com.example.day.core.core_features.memory.domain.model

/**
 * Domain model representing a category for long-term memory facts.
 * Categories are stored in lowercase for consistent lookup.
 *
 * @property id Unique identifier
 * @property title Category title in lowercase (e.g., "skills", "preferences", "experience")
 */
data class LTMCategory(
    val id: Long = 0,
    val title: String
)
