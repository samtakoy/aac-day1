package com.example.day.ragserver.pipeline.steps

import com.example.day.ragserver.pipeline.PipelineContext
import com.example.day.ragserver.pipeline.PipelineStep

/** Отсекает список результатов до finalTopK перед упаковкой в контекст. */
class TopKStep(private val topK: Int) : PipelineStep {
    override val name = "top_k"

    override suspend fun process(ctx: PipelineContext): PipelineContext =
        ctx.copy(results = ctx.results.take(topK))
}
