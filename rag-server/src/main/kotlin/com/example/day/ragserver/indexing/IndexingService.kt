package com.example.day.ragserver.indexing

import com.example.day.ragserver.config.RagConfig
import com.example.day.ragserver.db.CodeDatabase
import com.example.day.ragserver.embedding.EmbeddingProvider

class IndexingService(
    private val db: CodeDatabase,
    private val embeddingProvider: EmbeddingProvider,
    private val llmProvider: LlmProvider? = null,
) {
    private val metadataExtractor = llmProvider?.let { MetadataExtractor(it) }

    suspend fun indexAll(scanner: FileScanner, config: RagConfig) {
        val files = scanner.scan(config.codePath)
        val strategies = listOf(
            FixedSizeStrategy(),
            StructuralStrategy(),
        )
        for (strategy in strategies) {
            indexStrategy(strategy, files, config.forceReindex)
        }

        // Извлекаем метаданные если включено.
        // Инкрементальная логика: пропускаем уже обработанные классы,
        // принудительная перегенерация только при FORCE_REINDEX=true.
        if (config.extractMetadata && metadataExtractor != null) {
            extractMetadataForAll(files, config.forceReindex)
        }
    }

    private suspend fun indexStrategy(
        strategy: ChunkingStrategy,
        files: List<java.io.File>,
        forceReindex: Boolean,
    ) {
        val name = strategy.strategyName

        if (db.hasIndex(name) && !forceReindex) {
            println("IndexingService: skipping '$name' — index already exists (use FORCE_REINDEX=true to rebuild)")
            return
        }

        if (forceReindex && db.hasIndex(name)) {
            println("IndexingService: clearing old '$name' index")
            db.clearIndex(name)
        }

        println("IndexingService: indexing ${files.size} files with '$name' strategy...")
        var chunkCount = 0
        var errorCount = 0

        for (file in files) {
            val content = try {
                file.readText()
            } catch (e: Exception) {
                println("  [WARN] Cannot read file ${file.absolutePath}: ${e.message}")
                continue
            }

            val chunks = strategy.split(content, file.absolutePath, file.name)

            for (chunk in chunks) {
                val embedding = try {
                    embeddingProvider.embed(chunk.content)
                } catch (e: Exception) {
                    println("  [WARN] Embed failed for chunk ${chunk.chunkOrder} in ${file.name}: ${e.message}")
                    errorCount++
                    continue
                }

                try {
                    db.saveChunk(chunk, embedding)
                    chunkCount++
                } catch (e: Exception) {
                    println("  [WARN] Save failed for chunk ${chunk.chunkOrder} in ${file.name}: ${e.message}")
                    errorCount++
                }
            }
        }

        println("IndexingService: '$name' done — $chunkCount chunks saved, $errorCount errors")
    }

    private suspend fun extractMetadataForAll(files: List<java.io.File>, forceReindex: Boolean) {
        val ktFiles = files.filter { it.name.endsWith(".kt") }
        println("[Metadata] Starting extraction for ${ktFiles.size} Kotlin files...")

        // Ollama обрабатывает generate-запросы последовательно —
        // параллельные вызовы вызывают таймауты. Обрабатываем файлы по одному.
        var totalProcessed = 0
        var totalErrors = 0
        for (file in ktFiles) {
            val (p, e) = processFileMetadata(file, forceReindex)
            totalProcessed += p
            totalErrors += e
        }
        val totalSkipped = ktFiles.size - totalProcessed - totalErrors
        println("[Metadata] Done — $totalProcessed extracted, $totalSkipped skipped, $totalErrors errors")
    }

    // Обрабатывает один файл: один LLM-запрос на файл, className = имя файла.
    // Возвращает Pair(processed, errors). Skipped = файл не трогается (0, 0).
    private suspend fun processFileMetadata(
        file: java.io.File,
        forceReindex: Boolean,
    ): Pair<Int, Int> {
        val className = file.nameWithoutExtension

        if (!forceReindex && db.hasClassMetadata(className)) {
            println("  [Metadata] Skipping '$className' — already indexed")
            return 0 to 0
        }

        val content = try {
            file.readText()
        } catch (e: Exception) {
            println("  [WARN] Cannot read ${file.name}: ${e.message}")
            return 0 to 1
        }

        val metadata = metadataExtractor!!.extract(content, className)
            ?: return 0 to 1

        db.saveClassMetadata(metadata, file.absolutePath)

        try {
            val vector = embeddingProvider.embed(metadata.responsibility)
            db.saveMetadataVector(metadata.className, vector)
        } catch (e: Exception) {
            println("  [WARN] Metadata vector failed for '$className': ${e.message}")
        }

        return 1 to 0
    }
}
