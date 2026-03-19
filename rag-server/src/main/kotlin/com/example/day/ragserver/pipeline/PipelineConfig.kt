package com.example.day.ragserver.pipeline

data class PipelineConfig(
    val enableQueryOptimize: Boolean = false,       // query rewrite + translate
    val retrievalStrategy: RetrievalStrategy = RetrievalStrategy.TWO_STAGE,
    val chunkingStrategy: String = "structural",    // "structural"|"fixed", только для HYBRID
    val retrievalTopK: Int = 10,                    // top-K ДО фильтрации
    val threshold: Double = 0.0,                    // 0.0 = фильтр выключен
    val rerankStrategy: RerankStrategy = RerankStrategy.NONE,
    val finalTopK: Int = 5,                         // top-K ПОСЛЕ реранка → в LLM
)

enum class RetrievalStrategy { TWO_STAGE, HYBRID }
enum class RerankStrategy { NONE, HEURISTIC, LLM }

/**
 * Именованные конфигурации pipeline для сравнения.
 *
 * Поток данных для каждого пресета:
 *   Retrieve(retrievalTopK) → Filter(threshold) → Rerank → TopK(finalTopK) → Pack
 *
 * В debug-заголовке ответа видны все срезы:
 *   "Retrieved: 15 → Filtered: 9 → Final: 5"
 */
enum class PipelinePreset(val config: PipelineConfig) {

    /** Baseline: только двухэтапный поиск без фильтрации и реранкинга. */
    BASELINE(PipelineConfig(
        enableQueryOptimize = true,
        retrievalTopK = 10,
        finalTopK = 5,
    )),

    /** Threshold filter: убираем нерелевантные (score < 0.65) из 15 кандидатов. */
    FILTERED(PipelineConfig(
        enableQueryOptimize = true,
        retrievalTopK = 15,
        threshold = 0.66,
        finalTopK = 5,
    )),

    /** Heuristic rerank: keyword overlap boost поверх фильтрации. */
    RERANKED_HEURISTIC(PipelineConfig(
        enableQueryOptimize = true,
        retrievalTopK = 15,
        threshold = 0.66,
        rerankStrategy = RerankStrategy.HEURISTIC,
        finalTopK = 5,
    )),

    /** LLM rerank: локальная LLM оценивает релевантность каждого чанка.
     * Замечание: за реранкинг отвечает Llm - это ее задача отбросить нерелевантное.
     * Поэтому [threshold] тут только вредит, можно использовать 0.
     * Либо режимы:
     * reranked_strict: threshold=0.66 (для точечных запросов с именами классов)
     * reranked_recall: threshold=0.45 (для концептуальных/архитектурных вопросов)
     * */
    RERANKED_LLM(PipelineConfig(
        enableQueryOptimize = true,
        retrievalTopK = 15,
        threshold = 0.33, // 0.66,
        rerankStrategy = RerankStrategy.LLM,
        finalTopK = 5,
    ));

    companion object {
        fun fromString(s: String): PipelinePreset =
            entries.firstOrNull { it.name.equals(s, ignoreCase = true) } ?: BASELINE

        val all: List<PipelinePreset> get() = entries.toList()
    }
}
