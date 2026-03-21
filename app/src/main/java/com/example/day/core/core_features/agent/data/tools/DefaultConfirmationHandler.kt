package com.example.day.core.core_features.agent.data.tools

import com.example.day.core.core_features.agent.domain.tools.ConfirmationHandler
import com.example.day.core.core_features.agent.domain.tools.ConfirmationRequest
import com.example.day.core.core_features.agent.domain.tools.ToolCallingConstants
import javax.inject.Inject

internal class DefaultConfirmationHandler @Inject constructor() : ConfirmationHandler {

    override fun requiresConfirmation(
        toolName: String,
        arguments: String
    ): Boolean {
        val normalizedToolName = toolName.lowercase()
        return ToolCallingConstants.DANGEROUS_TOOL_KEYWORDS.any { keyword ->
            normalizedToolName.contains(keyword)
        }
    }

    override fun buildRequest(
        toolName: String,
        arguments: String,
        confirmationId: String
    ): ConfirmationRequest {
        val message = buildString {
            append("${ToolCallingConstants.CONFIRMATION_MESSAGE_PREFIX} '$toolName'")
            if (arguments.isNotBlank()) {
                append(" with arguments:\n")
                append(arguments)
            }
        }

        return ConfirmationRequest(
            confirmationId = confirmationId,
            title = ToolCallingConstants.CONFIRMATION_TITLE,
            message = message,
            actionLabel = toolName
        )
    }
}
