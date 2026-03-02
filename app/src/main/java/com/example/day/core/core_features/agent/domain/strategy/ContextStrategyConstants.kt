package com.example.day.core.core_features.agent.domain.strategy

/**
 * Constants for context strategy parameter names.
 * Used for passing parameters via Map to strategy.updateParams().
 */
object ContextStrategyConstants {
    /**
     * Parameter name for message limit.
     * - In SummarizationStrategy: represents msgLimit (how many pairs to keep as is)
     * - In SlidingWindowStrategy: represents windowSize (how many messages to keep)
     * - In StickyFactsStrategy: represents windowSize (how many messages to keep)
     */
    const val PARAM_MSG_LIMIT = "msg"

    /**
     * Parameter name for extra buffer limit.
     * Used only in SummarizationStrategy: how many extra pairs to accumulate before compression.
     */
    const val PARAM_EXTRA_LIMIT = "extra"

    /**
     * Parameter name for max facts limit.
     * Used only in StickyFactsStrategy: maximum number of facts to store.
     */
    const val PARAM_MAX_FACTS = "facts"

    /**
     * Parameter name for default branch ID.
     * Used in BranchingStrategy to set the default branch name.
     */
    const val PARAM_DEFAULT_BRANCH = "main"

    /**
     * Parameter name for branch ID.
     * Used in BranchingStrategy commands: new_branch, switch_branch, delete_branch.
     */
    const val PARAM_BRANCH_ID = "id"
}
