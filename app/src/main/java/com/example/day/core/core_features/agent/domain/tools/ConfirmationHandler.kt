package com.example.day.core.core_features.agent.domain.tools

interface ConfirmationHandler {
    fun requiresConfirmation(
        toolName: String,
        arguments: String
    ): Boolean

    fun buildRequest(
        toolName: String,
        arguments: String,
        confirmationId: String
    ): ConfirmationRequest
}
