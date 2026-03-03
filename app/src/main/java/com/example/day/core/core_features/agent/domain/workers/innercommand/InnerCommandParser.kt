package com.example.day.core.core_features.agent.domain.workers.innercommand

/**
 * Parser for extracting inner commands from user input.
 *
 * Parses commands in format: (commandName --param1 --param2 value2 --paramN valueN)
 * Parameters may or may not have values specified.
 */
class InnerCommandParser {

    companion object {
        // Pattern to recognize command in format (command_name --param value)
        private val COMMAND_PATTERN = Regex("""^\s*\((.*)\)\s*$""")
    }

    /**
     * Parses a command from the input string.
     *
     * Input format: (commandName --param1 --param2 value2 --paramN valueN)
     * - Parameter may have a value or be a flag without value
     *
     * @param input the raw user input string
     * @return InnerCommandParserResult containing the parsed command or an error
     */
    fun tryExtractCommand(input: String): InnerCommandParserResult {
        val content = input.trim()
        if (!content.startsWith("(")) {
            return InnerCommandParserResult.noCommand()
        }

        val matchResult = COMMAND_PATTERN.matchEntire(content)
        if (matchResult == null) {
            return InnerCommandParserResult.error("неверный формат команды - нет закрывающей скобки")
        }

        val innerContent = matchResult.groupValues[1].trim()
        val parts = innerContent.split("--")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (parts.isEmpty()) {
            return InnerCommandParserResult.error("параметры команды не указаны или неверный формат")
        }

        val commandName = parts[0].lowercase().trim()
        val params = parts.drop(1).mapNotNull { parseParameter(it) }

        return InnerCommandParserResult.success(
            InnerCommand(name = commandName, parameters = params)
        )
    }

    /**
     * Parses a parameter string into a key-value pair.
     * Format: "paramName" or "paramName value"
     *
     * @param param the parameter string
     * @return Pair of (key, value) or null if invalid
     */
    private fun parseParameter(param: String): Pair<String, String?>? {
        val pair = param.trim().split(" ", limit=2).filter { it.isNotBlank() }
        return when {
            pair.isEmpty() -> null
            pair.size > 1 -> Pair(pair[0], pair[1])
            else -> Pair(pair[0], null)
        }
    }
}
