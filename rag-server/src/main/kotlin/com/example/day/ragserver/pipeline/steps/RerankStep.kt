package com.example.day.ragserver.pipeline.steps

import com.example.day.ragserver.pipeline.PipelineContext
import com.example.day.ragserver.pipeline.PipelineStep
import com.example.day.ragserver.search.rerank.Reranker

class RerankStep(private val reranker: Reranker) : PipelineStep {
    override val name = "rerank"

    override suspend fun process(ctx: PipelineContext): PipelineContext {
        if (ctx.results.isEmpty()) return ctx
        val reranked = reranker.rerank(ctx.query, ctx.results)
        println("[Rerank] ${ctx.results.size} results reranked")
        // Сохраняем полный список до TopK — чтобы search handler мог определить что было отброшено
        return ctx.copy(results = reranked, resultsAfterRerank = reranked)
    }
}
