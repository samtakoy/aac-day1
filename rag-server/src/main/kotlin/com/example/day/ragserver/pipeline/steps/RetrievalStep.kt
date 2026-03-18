package com.example.day.ragserver.pipeline.steps

import com.example.day.ragserver.pipeline.PipelineConfig
import com.example.day.ragserver.pipeline.PipelineContext
import com.example.day.ragserver.pipeline.PipelineStep
import com.example.day.ragserver.pipeline.RetrievalStrategy
import com.example.day.ragserver.search.SearchService
import com.example.day.ragserver.search.TwoStageSearchService

class RetrievalStep(
    private val twoStageSearchService: TwoStageSearchService,
    private val searchService: SearchService,
    private val config: PipelineConfig,
) : PipelineStep {
    override val name = "retrieve"

    override suspend fun process(ctx: PipelineContext): PipelineContext {
        val results = when (config.retrievalStrategy) {
            RetrievalStrategy.TWO_STAGE ->
                twoStageSearchService.search(ctx.query, config.retrievalTopK)

            RetrievalStrategy.HYBRID ->
                searchService.search(
                    query = ctx.query,
                    strategy = config.chunkingStrategy,
                    topK = config.retrievalTopK,
                    useHybrid = true,
                )
        }
        return ctx.copy(
            results = results,
            metrics = ctx.metrics.copy(countAfterRetrieval = results.size),
        )
    }
}
