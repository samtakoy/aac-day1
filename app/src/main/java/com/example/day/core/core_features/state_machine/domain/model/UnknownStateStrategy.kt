package com.example.day.core.core_features.state_machine.domain.model

/**
 * Strategy for handling unknown states during deserialization or runtime.
 */
enum class UnknownStateStrategy {
    /** Throw an error when unknown state is encountered (default) */
    FAIL,

    /** Use fallback state instead of unknown state */
    FALLBACK,

    /** Ignore unknown state and return null */
    IGNORE,

    /** Use custom handler for unknown state */
    CUSTOM
}