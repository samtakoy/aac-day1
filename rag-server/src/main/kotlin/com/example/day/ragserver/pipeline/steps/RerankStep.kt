package com.example.day.ragserver.pipeline.steps

import com.example.day.ragserver.pipeline.PipelineContext
import com.example.day.ragserver.pipeline.PipelineStep
import com.example.day.ragserver.search.rerank.Reranker

class RerankStep(private val reranker: Reranker) : PipelineStep {
    override val name = "rerank"

    override suspend fun process(ctx: PipelineContext): PipelineContext {
        if (ctx.results.isEmpty()) return ctx
        // Реранкер получает ВСЕ результаты после фильтра и переранжирует их.
        // TopKStep отсекает финальный список — не здесь.
        val reranked = reranker.rerank(ctx.query, ctx.results)
        println("[Rerank] ${ctx.results.size} results reranked")
        return ctx.copy(
            results = reranked,
            metrics = ctx.metrics.copy(countAfterRerank = reranked.size),
        )
    }
}
