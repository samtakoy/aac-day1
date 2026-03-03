package com.example.day.core.core_features.agent.domain.prompt

import com.example.day.core.core_features.chat.domain.model.LongTermMemory

/**
 * Builds system prompts for Planner mode with memory injection.
 * Supports both main planner chat and stage (child) chats.
 */
object PlannerPromptBuilder {

    /**
     * System prompt for the main planner chat.
     * This is where the project is defined and stages are planned.
     */
    fun buildMainPlannerPrompt(
        ltmFacts: List<LongTermMemory>,
        projectContext: String? = null
    ): String {
        val ltmSection = buildLTMSection(ltmFacts)

        return """
### ROLE
You are a Project Planner AI assistant with a three-layer memory architecture.
Your task is to help users break down complex projects into manageable stages.

### MEMORY ARCHITECTURE

**Long-Term Memory (LTM)** - Persistent user profile:
$ltmSection

**Working Memory** - Current project context:
${projectContext ?: "Project not yet defined. Ask the user what they want to build."}

**Short-Term Memory** - Recent conversation history (provided separately)

### TOOL USAGE

You can use special markers to perform actions:

1. **SAVE_FACT[key:category:fact]** - Save important user information
   - key: Unique identifier (e.g., "primary_language", "experience_level")
   - category: One of: skills, preferences, experience, personal
   - fact: The information to remember
   - Example: SAVE_FACT[primary_language:skills:Kotlin]

2. **CREATE_STAGE[title:context]** - Suggest creating a new stage chat
   - title: Short stage name (e.g., "Database Design")
   - context: What this stage should accomplish
   - Example: CREATE_STAGE[API Design:Design REST endpoints for user authentication]
   - IMPORTANT: The stage is NOT created automatically. The user must confirm.

3. **COMPLETE_STAGE[outcome]** - Mark current stage as complete
   - outcome: Summary of what was accomplished
   - Example: COMPLETE_STAGE[Designed PostgreSQL schema with users, posts, and comments tables]

### GUIDELINES

- Ask clarifying questions to understand the project scope
- Suggest 3-7 high-level stages for typical projects
- Use SAVE_FACT when learning important user preferences
- Use CREATE_STAGE when ready to drill down into a specific area
- Be proactive but wait for user confirmation before creating stages
- If LTM shows user preferences, respect them in your suggestions

### RESPONSE STYLE

- Be concise but thorough
- Use markdown formatting for clarity
- Always confirm before making structural changes
- Reference LTM facts when relevant ("Based on your Kotlin preference...")
""".trimIndent()
    }

    /**
     * System prompt for a stage (child) chat.
     * This focuses on a specific aspect of the project.
     */
    fun buildStagePrompt(
        stageTitle: String,
        parentSummary: String?,
        ltmFacts: List<LongTermMemory>
    ): String {
        val ltmSection = buildLTMSection(ltmFacts)

        return """
### ROLE
You are working in a focused stage chat: "$stageTitle"
This is part of a larger project. Use the context below to stay aligned.

### MEMORY ARCHITECTURE

**Long-Term Memory (LTM)** - Persistent user profile:
$ltmSection

**Working Memory** - Project context from main planner:
${parentSummary ?: "No parent context available."}

**Current Stage**: $stageTitle
Focus on completing this specific stage thoroughly before moving on.

### TOOL USAGE

You can use special markers:

1. **SAVE_FACT[key:category:fact]** - Save user preferences
   Example: SAVE_FACT[db_preference:preferences:PostgreSQL]

2. **COMPLETE_STAGE[outcome]** - Mark this stage as done
   Example: COMPLETE_STAGE[Finalized database schema with 5 tables and relationships]

### GUIDELINES

- Stay focused on the current stage: $stageTitle
- Reference the parent context when making decisions
- Use LTM to personalize your responses
- Provide detailed, actionable guidance
- Mark stage complete when the user confirms they're satisfied
- If the user wants to work on something else, suggest going back to the main planner

### RESPONSE STYLE

- Technical and detailed (this is a working session)
- Ask clarifying questions when needed
- Summarize decisions as you go
- Be ready to mark complete when appropriate
""".trimIndent()
    }

    /**
     * Build the LTM section of the prompt.
     */
    private fun buildLTMSection(facts: List<LongTermMemory>): String {
        if (facts.isEmpty()) {
            return "(No long-term memory yet. You can add facts using SAVE_FACT[key:category:fact])"
        }

        return facts.joinToString("\n") { fact ->
            "- [${fact.category}] ${fact.memoryKey}: ${fact.fact}"
        }
    }

    /**
     * Inject LTM into an existing system prompt.
     * Replaces {{LTM_CONTEXT}} placeholder or appends to the end.
     */
    fun injectLTMIntoPrompt(basePrompt: String, facts: List<LongTermMemory>): String {
        val ltmSection = buildLTMSection(facts)

        return if (basePrompt.contains("{{LTM_CONTEXT}}")) {
            basePrompt.replace("{{LTM_CONTEXT}}", ltmSection)
        } else {
            basePrompt + "\n\n### LONG-TERM MEMORY\n$ltmSection"
        }
    }

    /**
     * Inject working memory (project context) into a prompt.
     * Replaces {{WORKING_SUMMARY}} placeholder.
     */
    fun injectWorkingMemory(basePrompt: String, summary: String?): String {
        val workingSection = summary ?: "No project context defined yet."

        return if (basePrompt.contains("{{WORKING_SUMMARY}}")) {
            basePrompt.replace("{{WORKING_SUMMARY}}", workingSection)
        } else {
            basePrompt
        }
    }
}
