package com.example.day.ragserver.pipeline.steps

import com.example.day.ragserver.pipeline.PipelineContext
import com.example.day.ragserver.pipeline.PipelineStep
import com.example.day.ragserver.search.QueryOptimizer

class QueryOptimizeStep(
    private val optimizer: QueryOptimizer,
    private val taskState: String? = null,
    private val history: String? = null,
) : PipelineStep {
    override val name = "query_optimize"

    override suspend fun process(ctx: PipelineContext): PipelineContext {
        val optimized = optimizer.optimizeWithContext(
            query = ctx.query,
            history = history,
        )
        return ctx.copy(
            query = optimized,
            metrics = ctx.metrics.copy(optimizedQuery = optimized),
        )
    }
}
