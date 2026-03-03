package com.example.day.core.core_features.agent.domain.tools

/**
 * Parses LLM responses for "pseudo tool calling" patterns.
 * Since we don't have native function calling support, we use pattern matching.
 *
 * Supported patterns:
 * - SAVE_FACT[key:category:fact] - Save to long-term memory
 * - CREATE_STAGE[title:context] - Suggest creating a stage chat (human-in-the-loop)
 * - COMPLETE_STAGE[outcome] - Mark stage as completed with artifact
 */
object ToolResponseParser {

    // Pattern: SAVE_FACT[key:category:fact] - markdown markers optional, case-insensitive
    private val SAVE_FACT_PATTERN = Regex(
        """(?:\*{1,2})?SAVE_FACT(?:\*{1,2})?\[([^:]+):([^:]+):([^\]]+)\]""",
        RegexOption.IGNORE_CASE
    )

    // Pattern: CREATE_STAGE[title:context] - markdown markers optional, case-insensitive
    private val CREATE_STAGE_PATTERN = Regex(
        """(?:\*{1,2})?CREATE_STAGE(?:\*{1,2})?\[([^:]+):([^\]]+)\]""",
        RegexOption.IGNORE_CASE
    )

    // Pattern: COMPLETE_STAGE[outcome] - markdown markers optional, case-insensitive
    private val COMPLETE_STAGE_PATTERN = Regex(
        """(?:\*{1,2})?COMPLETE_STAGE(?:\*{1,2})?\[([^\]]+)\]""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Result of parsing an LLM response.
     */
    data class ParsedResponse(
        val cleanedResponse: String,
        val saveFactCommands: List<SaveFactCommand>,
        val createStageCommands: List<CreateStageCommand>,
        val completeStageCommands: List<CompleteStageCommand>
    )

    data class SaveFactCommand(
        val memoryKey: String,
        val category: String,
        val fact: String
    )

    data class CreateStageCommand(
        val stageTitle: String,
        val workingSummary: String
    )

    data class CompleteStageCommand(
        val outcome: String
    )

    /**
     * Parse LLM response for tool patterns.
     * Returns cleaned response (with markers removed) and list of commands.
     */
    fun parse(response: String): ParsedResponse {
        val saveFactCommands = SAVE_FACT_PATTERN.findAll(response).map { match ->
            SaveFactCommand(
                memoryKey = match.groupValues[1].trim(),
                category = match.groupValues[2].trim(),
                fact = match.groupValues[3].trim()
            )
        }.toList()

        val createStageCommands = CREATE_STAGE_PATTERN.findAll(response).map { match ->
            CreateStageCommand(
                stageTitle = match.groupValues[1].trim(),
                workingSummary = match.groupValues[2].trim()
            )
        }.toList()

        val completeStageCommands = COMPLETE_STAGE_PATTERN.findAll(response).map { match ->
            CompleteStageCommand(
                outcome = match.groupValues[1].trim()
            )
        }.toList()

        // Remove all tool markers from response (including markdown variants and any case)
        val cleanedResponse = response
            .replace(SAVE_FACT_PATTERN, "")
            .replace(CREATE_STAGE_PATTERN, "")
            .replace(COMPLETE_STAGE_PATTERN, "")
            .replace(Regex("""(?:\*{1,2})?COMPLETE_STAGE(?:\*{1,2})?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""(?:\*{1,2})?CREATE_STAGE(?:\*{1,2})?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""(?:\*{1,2})?SAVE_FACT(?:\*{1,2})?""", RegexOption.IGNORE_CASE), "")
            .trim()
            .replace(Regex("\n{3,}"), "\n\n") // Remove excessive newlines

        return ParsedResponse(
            cleanedResponse = cleanedResponse,
            saveFactCommands = saveFactCommands,
            createStageCommands = createStageCommands,
            completeStageCommands = completeStageCommands
        )
    }

    /**
     * Check if response contains any tool patterns.
     */
    fun hasToolPatterns(response: String): Boolean {
        return SAVE_FACT_PATTERN.containsMatchIn(response) ||
               CREATE_STAGE_PATTERN.containsMatchIn(response) ||
               COMPLETE_STAGE_PATTERN.containsMatchIn(response)
    }
}
