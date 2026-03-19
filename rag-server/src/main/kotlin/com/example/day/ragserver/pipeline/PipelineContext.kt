package com.example.day.ragserver.pipeline

import com.example.day.ragserver.db.SearchResult
import com.example.day.ragserver.search.context.PackedContext

data class PipelineContext(
    val originalQuery: String,
    val query: String = originalQuery,      // обновляется QueryOptimizeStep
    val results: List<SearchResult> = emptyList(),
    val packed: PackedContext? = null,      // заполняется ContextPackingStep
    val metrics: PipelineMetrics = PipelineMetrics(),
)

data class PipelineMetrics(
    val timings: Map<String, Long> = emptyMap(),    // step name → ms
    val countAfterRetrieval: Int = 0,               // top-K ДО фильтра
    val countAfterFilter: Int = 0,                  // сколько прошло threshold
    // countAfterRerank убран: rerank не меняет количество, поле было равно countAfterFilter
    val optimizedQuery: String? = null,             // non-null если QueryOptimizeStep отработал
)
