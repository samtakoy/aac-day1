package com.example.day.ragserver.indexing

import com.example.day.ragserver.config.RagConfig
import com.example.day.ragserver.db.CodeDatabase
import com.example.day.ragserver.embedding.EmbeddingProvider

class IndexingService(
    private val db: CodeDatabase,
    private val embeddingProvider: EmbeddingProvider,
) {

    suspend fun indexAll(scanner: FileScanner, config: RagConfig) {
        val files = scanner.scan(config.codePath)
        val strategies = listOf(
            FixedSizeStrategy(),
            StructuralStrategy(),
        )
        for (strategy in strategies) {
            indexStrategy(strategy, files, config.forceReindex)
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
}
