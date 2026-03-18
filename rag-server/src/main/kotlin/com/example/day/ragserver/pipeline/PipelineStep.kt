package com.example.day.ragserver.pipeline

interface PipelineStep {
    val name: String
    suspend fun process(ctx: PipelineContext): PipelineContext
}
