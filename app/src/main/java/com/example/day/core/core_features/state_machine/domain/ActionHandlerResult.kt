package com.example.day.core.core_features.state_machine.domain

sealed interface ActionHandlerResult {
    data class LlmRequest(val prompt: String) : ActionHandlerResult
    data class Error(val message: String) : ActionHandlerResult
}