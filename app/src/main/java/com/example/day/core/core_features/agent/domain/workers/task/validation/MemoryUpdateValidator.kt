package com.example.day.core.core_features.agent.domain.workers.task.validation

import com.example.day.core.core_features.agent.domain.model.TaskMemoryKeys

/**
 * Validates memory updates from LLM to prevent corruption of LTM data.
 * Only allows specific key patterns to be written.
 */
class MemoryUpdateValidator {

    /**
     * Allowed key patterns for memory updates.
     * Uses regex to match valid keys.
     */
    private val allowedKeyPatterns = listOf(
        // Workflow keys
        Regex("^workflow:(current_state|current_step)$"),

        // Init phase keys
        Regex("^init:(title|description|goal|expert)$"),

        // Plan keys: plan:stage1, plan:stage1_desc, plan:stage1_expert, plan:stage1_status, plan:stage1_artifact
        Regex("^plan:stage\\d+$"),
        Regex("^plan:stage\\d+_(desc|expert|status|artifact)$"),
        Regex("^plan:total_stages$"),
        Regex("^plan:current_stage$"),

        // Verification keys: verif:stage1_feedback, verif:stage1_score
        Regex("^verif:stage\\d+_feedback$"),
        Regex("^verif:stage\\d+_score$"),

        // Exec keys: exec:stage1_result
        Regex("^exec:stage\\d+_result$")
    )

    /**
     * Maximum allowed value length to prevent abuse.
     */
    companion object {
        const val MAX_KEY_LENGTH = 100
        const val MAX_VALUE_LENGTH = 10000
    }

    /**
     * Validate a single memory update.
     *
     * @param key Memory key
     * @param value Memory value
     * @return ValidationResult
     */
    fun validate(key: String, value: String): ValidationResult {
        // Check key length
        if (key.length > MAX_KEY_LENGTH) {
            return ValidationResult.Invalid(
                "Key too long: ${key.length} > $MAX_KEY_LENGTH"
            )
        }

        // Check value length
        if (value.length > MAX_VALUE_LENGTH) {
            return ValidationResult.Invalid(
                "Value too long for key '$key': ${value.length} > $MAX_VALUE_LENGTH"
            )
        }

        // Check key matches allowed patterns
        val isAllowed = allowedKeyPatterns.any { pattern ->
            pattern.matches(key)
        }

        return if (isAllowed) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                "Key '$key' does not match allowed patterns. " +
                    "Allowed prefixes: workflow:, init:, plan:, verif:, exec:"
            )
        }
    }

    /**
     * Validate multiple memory updates.
     *
     * @param updates Map of key-value pairs
     * @return ValidationResult with all errors if any
     */
    fun validateAll(updates: Map<String, String>): ValidationResult {
        val errors = mutableListOf<String>()

        updates.forEach { (key, value) ->
            when (val result = validate(key, value)) {
                is ValidationResult.Valid -> { /* ok */ }
                is ValidationResult.Invalid -> errors.add(result.error)
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors.joinToString("; "))
        }
    }

    /**
     * Filter updates to only include valid ones.
     *
     * @param updates Map of key-value pairs
     * @return Filtered map with only valid updates
     */
    fun filterValid(updates: Map<String, String>): Map<String, String> {
        return updates.filter { (key, value) ->
            validate(key, value) is ValidationResult.Valid
        }
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val error: String) : ValidationResult()
    }
}