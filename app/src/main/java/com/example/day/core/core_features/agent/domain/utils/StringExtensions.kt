package com.example.day.core.core_features.agent.domain.utils

/**
 * Extension functions for string manipulation used by workers.
 */
internal fun String.trimCmd(): String = this.trim()

internal fun String.ifBlank(default: () -> String): String = 
    if (this.isBlank()) default() else this

internal fun String?.extractFromStartBrackets(): String? {
    if (this == null) return null
    val start = this.indexOf('(')
    val end = this.indexOf(')')
    return if (start >= 0 && end > start) {
        this.substring(start + 1, end).trim()
    } else {
        null
    }
}
