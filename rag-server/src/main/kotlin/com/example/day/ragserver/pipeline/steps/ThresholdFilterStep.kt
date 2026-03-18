package com.example.day.ragserver.pipeline.steps

import com.example.day.ragserver.pipeline.PipelineContext
import com.example.day.ragserver.pipeline.PipelineStep

class ThresholdFilterStep(private val threshold: Double) : PipelineStep {
    override val name = "filter"

    override suspend fun process(ctx: PipelineContext): PipelineContext {
        val filtered = ctx.results.filter { it.score >= threshold }
        println("[Filter] threshold=$threshold: ${ctx.results.size} → ${filtered.size} results")
        return ctx.copy(
            results = filtered,
            metrics = ctx.metrics.copy(countAfterFilter = filtered.size),
        )
    }
}
