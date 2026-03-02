package com.example.day.core.core_features.agent.domain.workers.innercommand.handler

import com.example.day.core.core_features.chat.domain.model.Chat

/**
 * Interface for handling inner commands extracted from user input.
 */
interface CommandHandler {
    /**
     * The command name this handler processes (e.g., "info", "setup")
     */
    val commandName: String

    /**
     * Handles the command with given parameters.
     *
     * @param params List of parameter key-value pairs
     * @param chat The current chat context
     * @return Result of command execution
     */
    suspend fun handle(
        params: List<Pair<String, String?>>,
        chat: Chat
    ): CommandResult
}
