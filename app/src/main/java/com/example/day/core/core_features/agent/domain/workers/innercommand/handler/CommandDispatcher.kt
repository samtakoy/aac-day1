package com.example.day.core.core_features.agent.domain.workers.innercommand.handler

import com.example.day.core.core_features.agent.domain.workers.innercommand.InnerCommand
import com.example.day.core.core_features.chat.domain.model.Chat
import com.example.day.core.core_features.chat.domain.tools.ChatTools
import javax.inject.Inject

/**
 * Dispatches inner commands to appropriate handlers.
 */
class CommandDispatcher @Inject constructor(
    handlers: @JvmSuppressWildcards Set<CommandHandler>,
    private val chatTools: ChatTools
) {
    private val handlerMap = handlers.associateBy { it.commandName }

    /**
     * Dispatches a command to its handler.
     *
     * @param command The parsed inner command
     * @param chat The current chat context
     * @return true if command was handled, false if no handler found
     */
    suspend fun dispatch(command: InnerCommand, chat: Chat): Boolean {
        val handler = handlerMap[command.name] ?: return false

        return when (val result = handler.handle(command.parameters, chat)) {
            is CommandResult.Success -> {
                result.message?.let { chatTools.addInfoMessage(chat.id, it) }
                true
            }
            is CommandResult.Error -> {
                chatTools.addBotMessage(chat.id, result.message)
                true
            }
            CommandResult.NotHandled -> false
        }
    }
}
