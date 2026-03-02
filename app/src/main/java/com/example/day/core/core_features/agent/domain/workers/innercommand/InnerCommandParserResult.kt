package com.example.day.core.core_features.agent.domain.workers.innercommand

/**
 * Result of parsing an inner command from user input.
 *
 * If both [command] and [error] are null - there was no command in the input string.
 * If [command] is not null - the command was successfully parsed.
 * If [error] is not null - there was a parsing error.
 */
data class InnerCommandParserResult(
    val command: InnerCommand?,
    val error: String?
) {
    companion object {
        /**
         * Creates a result indicating no command was found in the input.
         */
        fun noCommand(): InnerCommandParserResult = InnerCommandParserResult(
            command = null,
            error = null
        )

        /**
         * Creates a result with a successfully parsed command.
         */
        fun success(command: InnerCommand): InnerCommandParserResult = InnerCommandParserResult(
            command = command,
            error = null
        )

        /**
         * Creates a result with a parsing error.
         */
        fun error(message: String): InnerCommandParserResult = InnerCommandParserResult(
            command = null,
            error = message
        )
    }
}
