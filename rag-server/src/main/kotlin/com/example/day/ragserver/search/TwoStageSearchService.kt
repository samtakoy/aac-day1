package com.example.day.ragserver.search

import com.example.day.ragserver.db.ClassMetadata
import com.example.day.ragserver.db.CodeDatabase
import com.example.day.ragserver.db.SearchResult
import com.example.day.ragserver.embedding.EmbeddingProvider
import com.example.day.ragserver.logging.SessionLogger

class TwoStageSearchService(
    private val db: CodeDatabase,
    private val embeddingProvider: EmbeddingProvider,
    private val sessionLogger: SessionLogger? = null,
) {
    companion object {
        const val COARSE_TOP_K = 8
        const val DRILL_DOWN_PER_CLASS = 3

        // Максимальный вклад keyword-буста в Stage 1.
        // Keyword не может "перебить" семантику — только усилить точные совпадения.
        const val KEYWORD_BOOST_MAX = 0.2
    }

    suspend fun search(query: String, topK: Int): List<SearchResult> {
        val allMetadata = db.getAllClassMetadata()
        val metadataVectors = db.getAllMetadataVectors()
        val queryVector = embeddingProvider.embed(query)

        println("[TwoStage] DB stats: structural=${db.getStats().structuralChunks}, metadata=${allMetadata.size}, metadataVectors=${metadataVectors.size}")

        if (allMetadata.isEmpty()) {
            println("[TwoStage] No class metadata — falling back to standard search")
            val fallbackResults = standardSearch(queryVector, topK)
            println("[TwoStage] standardSearch returned ${fallbackResults.size} chunks")
            sessionLogger?.logTwoStageSearch(
                query = query,
                metadataCount = 0,
                metadataVectorCount = 0,
                fallback = true,
                stage1Classes = emptyList(),
                stage2Details = emptyList(),
                finalChunks = fallbackResults.map { "${it.chunk.fileName}:${it.chunk.startLine} ${it.chunk.declarationName ?: ""}" to it.score.toDouble() },
            )
            return fallbackResults
        }

        val stage1WithScores = findRelevantClassesWithScores(query, queryVector, allMetadata, metadataVectors)
        val relevantClasses = stage1WithScores.map { (meta, _) -> meta }
        println("[TwoStage] Stage 1: ${relevantClasses.size} classes: ${relevantClasses.map { it.className }}")

        if (relevantClasses.isEmpty()) {
            println("[TwoStage] Stage 1 returned 0 classes — falling back to standard search")
            val fallbackResults = standardSearch(queryVector, topK)
            println("[TwoStage] standardSearch returned ${fallbackResults.size} chunks")
            sessionLogger?.logTwoStageSearch(
                query = query,
                metadataCount = allMetadata.size,
                metadataVectorCount = metadataVectors.size,
                fallback = true,
                stage1Classes = emptyList(),
                stage2Details = emptyList(),
                finalChunks = fallbackResults.map { "${it.chunk.fileName}:${it.chunk.startLine} ${it.chunk.declarationName ?: ""}" to it.score.toDouble() },
            )
            return fallbackResults
        }

        val (results, stage2Details) = drillDownWithDetails(queryVector, relevantClasses)
        println("[TwoStage] Stage 2: ${results.size} chunks found")

        val final = results
            .distinctBy { it.chunk.id }
            .sortedByDescending { it.score }
            .take(topK)
        println("[TwoStage] Final: ${final.size} chunks")
        final.forEachIndexed { i, r ->
            println("[TwoStage]   [$i] score=${r.score} file=${r.chunk.fileName} decl=${r.chunk.declarationName} preview=${r.chunk.content.take(60).replace('\n', ' ')}")
        }

        sessionLogger?.logTwoStageSearch(
            query = query,
            metadataCount = allMetadata.size,
            metadataVectorCount = metadataVectors.size,
            fallback = false,
            stage1Classes = stage1WithScores.map { (meta, score) -> meta.className to score },
            stage2Details = stage2Details,
            finalChunks = final.map { "${it.chunk.fileName}:${it.chunk.startLine} ${it.chunk.declarationName ?: ""}" to it.score.toDouble() },
        )
        return final
    }

    /**
     * Stage 1: выбираем наиболее релевантные классы по их метаданным.
     * Возвращает пары (ClassMetadata, finalScore) для логирования.
     */
    private fun findRelevantClassesWithScores(
        query: String,
        queryVector: FloatArray,
        allMetadata: List<ClassMetadata>,
        metadataVectors: List<Pair<String, FloatArray>>,
    ): List<Pair<ClassMetadata, Double>> {
        return if (metadataVectors.isNotEmpty()) {
            findRelevantClassesByEmbeddingWithScores(queryVector, allMetadata, metadataVectors, tokenize(query), query)
        } else {
            println("[TwoStage] No metadata vectors — using keyword-only Stage 1")
            findRelevantClassesByKeywordWithScores(query, allMetadata)
        }
    }

    /**
     * Embedding-based Stage 1 (основной путь).
     * Score = embeddingScore + min(keywordScore * KEYWORD_BOOST_MAX, KEYWORD_BOOST_MAX)
     */
    private fun findRelevantClassesByEmbeddingWithScores(
        queryVector: FloatArray,
        allMetadata: List<ClassMetadata>,
        metadataVectors: List<Pair<String, FloatArray>>,
        queryTokens: List<String> = emptyList(),
        rawQuery: String = "",
    ): List<Pair<ClassMetadata, Double>> {
        val vectorMap = metadataVectors.toMap()

        return allMetadata
            .mapNotNull { meta ->
                val vector = vectorMap[meta.className] ?: return@mapNotNull null
                val embScore = VectorMath.cosineSimilarity(queryVector, vector).toDouble()

                val kwBoost = if (queryTokens.isNotEmpty()) {
                    val kwScore = computeKeywordScore(queryTokens, meta)
                    minOf(kwScore * KEYWORD_BOOST_MAX, KEYWORD_BOOST_MAX)
                } else 0.0

                // Exact class name match — query is "AIAgent" and class is "AIAgent".
                // Overcomes the embedding pollution from many similarly-named library classes.
                val exactBoost = if (rawQuery.isNotEmpty() && meta.className.equals(rawQuery.trim(), ignoreCase = true)) 1.0 else 0.0

                val finalScore = embScore + kwBoost + exactBoost
                if (embScore > 0.1) meta to finalScore else null
            }
            .sortedByDescending { (_, score) -> score }
            .take(COARSE_TOP_K)
    }

    /**
     * Keyword-only Stage 1 (fallback если metadata vectors не сгенерированы).
     */
    private fun findRelevantClassesByKeywordWithScores(
        query: String,
        allMetadata: List<ClassMetadata>,
    ): List<Pair<ClassMetadata, Double>> {
        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return allMetadata.take(COARSE_TOP_K).map { it to 0.0 }

        return allMetadata
            .mapNotNull { meta ->
                val score = computeKeywordScore(queryTokens, meta)
                if (score > 0.0) meta to score else null
            }
            .sortedByDescending { (_, score) -> score }
            .take(COARSE_TOP_K)
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
     * Возвращает результаты + детали для логирования.
     */
    private fun drillDownWithDetails(
        queryVector: FloatArray,
        relevantClasses: List<ClassMetadata>,
    ): Pair<List<SearchResult>, List<SessionLogger.Stage2ChunkDetail>> {
        val allStructural = db.getAllVectors("structural")
        println("[TwoStage] drillDown: allStructural=${allStructural.size} chunks")
        val results = mutableListOf<SearchResult>()
        val details = mutableListOf<SessionLogger.Stage2ChunkDetail>()
        val keyMethodNames = relevantClasses.flatMap { it.keyMethods }.map { it.name.lowercase() }.toSet()

        for (classMeta in relevantClasses) {
            val exactMatch = allStructural.filter { (chunk, _) ->
                chunk.fileName.removeSuffix(".kt").equals(classMeta.className, ignoreCase = true) ||
                    chunk.declarationName?.equals(classMeta.className, ignoreCase = true) == true ||
                    chunk.parentScope?.contains(classMeta.className, ignoreCase = true) == true
            }
            // Soft-match only when className is a specific enough identifier (length >= 6).
            // Short names like "Failure", "Error" match too many unrelated chunks.
            val classChunks = exactMatch.ifEmpty {
                if (classMeta.className.length >= 6) {
                    allStructural.filter { (chunk, _) ->
                        chunk.content.contains(classMeta.className)
                    }
                } else emptyList()
            }
            val softMatchCount = if (exactMatch.isEmpty()) classChunks.size else 0
            println("[TwoStage]   class=${classMeta.className} exactMatch=${exactMatch.size} softMatch=$softMatchCount total=${classChunks.size}")

            val scored = classChunks
                .map { (chunk, vector) ->
                    val embScore = VectorMath.cosineSimilarity(queryVector, vector).toDouble()
                    val methodBoost = if (chunk.declarationName?.lowercase() in keyMethodNames) 0.1 else 0.0
                    SearchResult(chunk, (embScore + methodBoost).toFloat())
                }
                .sortedByDescending { it.score }
                .take(DRILL_DOWN_PER_CLASS)

            results.addAll(scored)
            details.add(SessionLogger.Stage2ChunkDetail(
                className = classMeta.className,
                exactMatches = exactMatch.size,
                softMatches = softMatchCount,
                topChunks = scored.map { Triple(it.chunk.fileName, it.chunk.startLine, it.score.toDouble()) },
            ))
        }

        return results to details
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
