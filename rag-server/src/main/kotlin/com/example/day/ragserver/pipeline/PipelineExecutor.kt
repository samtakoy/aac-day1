package com.example.day.ragserver.pipeline

class PipelineExecutor(private val steps: List<PipelineStep>) {

    suspend fun execute(query: String): PipelineContext {
        println("[Pipeline] START query='${query.take(80)}' steps=${steps.map { it.name }}")
        var ctx = PipelineContext(originalQuery = query)
        for (step in steps) {
            println("[Pipeline] step=${step.name} START")
            val start = System.currentTimeMillis()
            ctx = try {
                step.process(ctx)
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - start
                println("[Pipeline] step=${step.name} ERROR after ${elapsed}ms: ${e.javaClass.simpleName}: ${e.message}")
                throw e
            }
            val elapsed = System.currentTimeMillis() - start
            println("[Pipeline] step=${step.name} DONE ${elapsed}ms results=${ctx.results.size}")
            ctx = ctx.copy(
                metrics = ctx.metrics.copy(
                    timings = ctx.metrics.timings + (step.name to elapsed)
                )
            )
        }
        println("[Pipeline] DONE total_results=${ctx.results.size}")
        return ctx
    }
}
