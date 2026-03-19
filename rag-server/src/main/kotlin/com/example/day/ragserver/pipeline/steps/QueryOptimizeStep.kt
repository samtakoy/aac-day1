package com.example.day.ragserver.pipeline.steps

import com.example.day.ragserver.pipeline.PipelineContext
import com.example.day.ragserver.pipeline.PipelineStep
import com.example.day.ragserver.search.QueryOptimizer

class QueryOptimizeStep(private val optimizer: QueryOptimizer) : PipelineStep {
    override val name = "query_optimize"

    override suspend fun process(ctx: PipelineContext): PipelineContext {
        val optimized = optimizer.optimize(ctx.query)
        return ctx.copy(
            query = optimized,
            metrics = ctx.metrics.copy(optimizedQuery = optimized),
        )
    }
}
