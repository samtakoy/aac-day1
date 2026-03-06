package com.example.day.core.core_features.agent.domain.strategy

import com.example.day.core.core_features.agent.domain.model.AContextParams
import com.example.day.core.core_features.agent.domain.strategy.impl.ContextEmptyStrategy
import com.example.day.core.core_features.agent.domain.strategy.impl.ContextFullStrategy
import com.example.day.core.core_features.agent.domain.strategy.impl.ContextSlidingWindowStrategy
import com.example.day.core.core_features.agent.domain.strategy.impl.ContextStickyFactsStrategy
import com.example.day.core.core_features.agent.domain.strategy.impl.ContextSummaryStrategy
import com.example.day.core.core_features.agent.domain.strategy.impl.ContextBranchingStrategy
import com.example.day.core.core_features.llm.domain.LlmRequestUseCase
import javax.inject.Inject

class StrategyFactory @Inject constructor(
    private val llmRequestUseCase: LlmRequestUseCase
) {

    /** Create strategy by enum type (used by AIAgentFactory). */
    fun create(type: CtxStrategyType): ContextStrategy = when (type) {
        CtxStrategyType.FULL_CONTEXT -> ContextFullStrategy()
        CtxStrategyType.SUMMARIZATION -> ContextSummaryStrategy(llmRequestUseCase)
        CtxStrategyType.SLIDING_WINDOW -> ContextSlidingWindowStrategy()
        CtxStrategyType.STICKY_FACTS -> ContextStickyFactsStrategy(llmRequestUseCase)
        CtxStrategyType.BRANCHING -> ContextBranchingStrategy()
        CtxStrategyType.EMPTY -> ContextEmptyStrategy()
    }

    /** Create strategy by context params (kept for other callers). */
    fun create(aContext: AContextParams): ContextStrategy = when (aContext) {
        AContextParams.Empty -> ContextEmptyStrategy()
        AContextParams.Full -> ContextFullStrategy()
        is AContextParams.SlidingWindow -> ContextSlidingWindowStrategy()
        is AContextParams.Summarization -> ContextSummaryStrategy(llmRequestUseCase)
        is AContextParams.StickyFacts -> ContextStickyFactsStrategy(llmRequestUseCase)
        is AContextParams.Branching -> ContextBranchingStrategy()
    }
}
