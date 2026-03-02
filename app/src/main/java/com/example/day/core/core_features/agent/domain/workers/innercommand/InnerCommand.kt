package com.example.day.core.core_features.agent.domain.workers.innercommand

/**
 * Represents a parsed inner command extracted from user input.
 *
 * @param name the name of the command
 * @param parameters list of extracted parameters as key-value pairs
 *                   (key is the parameter name, value is the optional parameter value)
 */
class InnerCommand(
    val name: String,
    val parameters: List<Pair<String, String?>>
)
