package com.example.day.core.core_features.state_machine.domain.model

import kotlinx.serialization.Serializable

/**
 * Type-safe value class for state identification.
 * Provides compile-time safety against typos while maintaining runtime efficiency.
 *
 * Usage:
 * ```
 * val state = StateId.INIT
 * val customState = StateId.of("my_custom_state")
 * ```
 */
@JvmInline
@Serializable
value class StateId(val value: String) {
    companion object {
        /**
         * Factory method to create StateId from string with validation.
         * Normalizes to lowercase and trims whitespace.
         */
        fun of(value: String): StateId {
            return StateId(value.lowercase().trim())
        }
    }

    override fun toString(): String = value
}