package com.example.day.core.core_features.agent.domain.tools

/**
 * Lifecycle of a single execution run.
 */
enum class ExecutionSessionStatus {
    Running,
    WaitingUserConfirmation,
    Completed,
    Failed,
    Canceled
}
