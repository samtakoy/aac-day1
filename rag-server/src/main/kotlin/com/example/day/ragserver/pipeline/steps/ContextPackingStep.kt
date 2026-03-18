package com.example.day.ragserver.pipeline.steps

import com.example.day.ragserver.pipeline.PipelineContext
import com.example.day.ragserver.pipeline.PipelineStep
import com.example.day.ragserver.search.context.ContextPacker

/** Упаковывает результаты в контекст для LLM. TopK уже применён TopKStep. */
class ContextPackingStep(private val packer: ContextPacker) : PipelineStep {
    override val name = "pack"

    override suspend fun process(ctx: PipelineContext): PipelineContext =
        ctx.copy(packed = packer.pack(ctx.results))
}
