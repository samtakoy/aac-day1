package com.example.day.core.core_features.agent.domain

/**
 * Defines the behavior mode of an AI agent.
 * Used to select the appropriate Worker and configure agent behavior.
 *
 * Note: The existing Worker pattern already provides behavior mode selection
 * through different Workers (SimpleWorker, TalkWorker, TaskWorker, etc.).
 * This enum documents the available modes for consistency with Koog framework.
 */
enum class AgentBehaviorMode {
    /**
     * One-shot request with no conversation history.
     * Uses SimpleWorker for direct LLM execution without context.
     */
    SINGLE_RUN,

    /**
     * Conversational interaction with memory and context compression.
     * Uses TalkWorker for ongoing conversations with context management.
     */
    CONVERSATIONAL,

    /**
     * Multi-step task planning and execution.
     * Uses TaskWorker for complex tasks requiring planning and verification.
     */
    TASK_ORCHESTRATOR
}
