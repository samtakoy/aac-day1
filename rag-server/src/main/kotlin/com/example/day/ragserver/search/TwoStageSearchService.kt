package com.example.day.ragserver.search

import com.example.day.ragserver.db.ClassMetadata
import com.example.day.ragserver.db.CodeDatabase
import com.example.day.ragserver.db.SearchResult
import com.example.day.ragserver.embedding.EmbeddingProvider

class TwoStageSearchService(
    private val db: CodeDatabase,
    private val embeddingProvider: EmbeddingProvider,
) {
    companion object {
        const val COARSE_TOP_K = 5
        const val DRILL_DOWN_PER_CLASS = 3

        // Максимальный вклад keyword-буста в Stage 1.
        // Keyword не может "перебить" семантику — только усилить точные совпадения.
        const val KEYWORD_BOOST_MAX = 0.2
    }

    suspend fun search(query: String, topK: Int): List<SearchResult> {
        val allMetadata = db.getAllClassMetadata()
        val queryVector = embeddingProvider.embed(query)

        if (allMetadata.isEmpty()) {
            println("[TwoStage] No class metadata — falling back to standard search")
            return standardSearch(queryVector, topK)
        }

        val relevantClasses = findRelevantClasses(query, queryVector, allMetadata)
        println("[TwoStage] Stage 1: ${relevantClasses.size} classes: ${relevantClasses.map { it.className }}")

        if (relevantClasses.isEmpty()) {
            return standardSearch(queryVector, topK)
        }

        val results = drillDown(queryVector, relevantClasses)
        println("[TwoStage] Stage 2: ${results.size} chunks found")

        return results
            .distinctBy { it.chunk.id }
            .sortedByDescending { it.score }
            .take(topK)
    }

    /**
     * Stage 1: выбираем наиболее релевантные классы по их метаданным.
     *
     * Если в БД есть векторы метаданных — используем embedding similarity как основной сигнал,
     * дополняя keyword-бустом для точных совпадений имён классов/методов.
     *
     * Fallback на чистый keyword-поиск если векторы ещё не сгенерированы
     * (например, при первом запуске до индексации метаданных).
     */
    private suspend fun findRelevantClasses(
        query: String,
        queryVector: FloatArray,
        allMetadata: List<ClassMetadata>,
    ): List<ClassMetadata> {
        val metadataVectors = db.getAllMetadataVectors()

        return if (metadataVectors.isNotEmpty()) {
            findRelevantClassesByEmbedding(queryVector, allMetadata, metadataVectors, tokenize(query))
        } else {
            println("[TwoStage] No metadata vectors — using keyword-only Stage 1")
            findRelevantClassesByKeyword(query, allMetadata)
        }
    }

    /**
     * Embedding-based Stage 1 (основной путь).
     *
     * Score = embeddingScore + min(keywordScore * KEYWORD_BOOST_MAX, KEYWORD_BOOST_MAX)
     *
     * Логика буста:
     * - Русский запрос → keywordScore = 0 → score = embeddingScore. Корректно.
     * - Английский запрос с точным именем класса → keyword даёт буст до +0.2.
     *   Это важно когда пользователь знает имя класса и хочет точное попадание.
     * - Буст ограничен KEYWORD_BOOST_MAX — семантика всегда управляет рейтингом.
     */
    private fun findRelevantClassesByEmbedding(
        queryVector: FloatArray,
        allMetadata: List<ClassMetadata>,
        metadataVectors: List<Pair<String, FloatArray>>,
        queryTokens: List<String> = emptyList(),
    ): List<ClassMetadata> {
        val vectorMap = metadataVectors.toMap()

        return allMetadata
            .mapNotNull { meta ->
                val vector = vectorMap[meta.className] ?: return@mapNotNull null
                val embScore = VectorMath.cosineSimilarity(queryVector, vector).toDouble()

                // Keyword буст: усиливает точные совпадения имён, не перекрывает семантику
                val kwBoost = if (queryTokens.isNotEmpty()) {
                    val kwScore = computeKeywordScore(queryTokens, meta)
                    minOf(kwScore * KEYWORD_BOOST_MAX, KEYWORD_BOOST_MAX)
                } else 0.0

                val finalScore = embScore + kwBoost

                // Минимальный порог по embedding — отсекаем заведомо нерелевантные классы
                if (embScore > 0.1) meta to finalScore else null
            }
            .sortedByDescending { (_, score) -> score }
            .take(COARSE_TOP_K)
            .map { (meta, _) -> meta }
    }

    /**
     * Keyword-only Stage 1 (fallback если metadata vectors не сгенерированы).
     * Поиск по responsibility + domainTags + className + keyMethods.
     * Эффективен только для английских запросов с точными именами.
     */
    private fun findRelevantClassesByKeyword(
        query: String,
        allMetadata: List<ClassMetadata>,
    ): List<ClassMetadata> {
        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return allMetadata.take(COARSE_TOP_K)

        return allMetadata
            .mapNotNull { meta ->
                val score = computeKeywordScore(queryTokens, meta)
                if (score > 0.0) meta to score else null
            }
            .sortedByDescending { (_, score) -> score }
            .take(COARSE_TOP_K)
            .map { (meta, _) -> meta }
    }

    // Считает долю токенов запроса, найденных в текстовых полях метаданных класса
    private fun computeKeywordScore(queryTokens: List<String>, meta: ClassMetadata): Double {
        if (queryTokens.isEmpty()) return 0.0
        val searchText = buildSearchText(meta)
        return queryTokens.count { token -> searchText.contains(token) }.toDouble() / queryTokens.size
    }

    // Объединяет все текстовые поля метаданных в одну строку для keyword-поиска
    private fun buildSearchText(meta: ClassMetadata): String = buildString {
        append(meta.responsibility); append(" ")
        append(meta.domainTags.joinToString(" ")); append(" ")
        append(meta.className); append(" ")
        append(meta.keyMethods.joinToString(" ") { it.name + " " + it.description })
    }.lowercase()

    /**
     * Stage 2: embedding-поиск по чанкам внутри отобранных классов.
     */
    private fun drillDown(
        queryVector: FloatArray,
        relevantClasses: List<ClassMetadata>,
    ): List<SearchResult> {
        val allStructural = db.getAllVectors("structural")
        val results = mutableListOf<SearchResult>()
        val keyMethodNames = relevantClasses.flatMap { it.keyMethods }.map { it.name.lowercase() }.toSet()

        for (classMeta in relevantClasses) {
            val classChunks = allStructural.filter { (chunk, _) ->
                chunk.fileName.removeSuffix(".kt").equals(classMeta.className, ignoreCase = true) ||
                    chunk.declarationName?.equals(classMeta.className, ignoreCase = true) == true
            }.ifEmpty {
                // Soft match: имя класса встречается где-то в содержимом чанка
                allStructural.filter { (chunk, _) ->
                    chunk.content.contains(classMeta.className)
                }
            }

            val scored = classChunks
                .map { (chunk, vector) ->
                    val embScore = VectorMath.cosineSimilarity(queryVector, vector).toDouble()
                    val methodBoost = if (chunk.declarationName?.lowercase() in keyMethodNames) 0.1 else 0.0
                    SearchResult(chunk, (embScore + methodBoost).toFloat())
                }
                .sortedByDescending { it.score }
                .take(DRILL_DOWN_PER_CLASS)

            results.addAll(scored)
        }

        return results
    }

    private fun standardSearch(queryVector: FloatArray, topK: Int): List<SearchResult> {
        return db.getAllVectors("structural")
            .map { (chunk, vector) ->
                SearchResult(chunk, VectorMath.cosineSimilarity(queryVector, vector))
            }
            .sortedByDescending { it.score }
            .take(topK)
    }

    private fun tokenize(text: String): List<String> = text
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .lowercase()
        .split(Regex("[\\s\\-_.,:;()\\[\\]{}]+"))
        .filter { it.length > 2 }
}
