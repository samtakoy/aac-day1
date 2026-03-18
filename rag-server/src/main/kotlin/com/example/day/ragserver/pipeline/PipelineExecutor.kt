package com.example.day.ragserver.pipeline

class PipelineExecutor(private val steps: List<PipelineStep>) {

    suspend fun execute(query: String): PipelineContext {
        var ctx = PipelineContext(originalQuery = query)
        for (step in steps) {
            val start = System.currentTimeMillis()
            ctx = step.process(ctx)
            val elapsed = System.currentTimeMillis() - start
            ctx = ctx.copy(
                metrics = ctx.metrics.copy(
                    timings = ctx.metrics.timings + (step.name to elapsed)
                )
            )
        }
        return ctx
    }
}
