package com.example.day.core.core_features.agent.domain.workers.innercommand.handler

sealed class CommandResult {
    data class Success(val message: String? = null) : CommandResult()
    data class Error(val message: String) : CommandResult()
    object NotHandled : CommandResult()
}
