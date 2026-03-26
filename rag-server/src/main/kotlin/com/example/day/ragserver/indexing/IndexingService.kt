package com.example.day.ragserver.indexing

import com.example.day.ragserver.config.RagConfig
import com.example.day.ragserver.db.ClassMetadata
import com.example.day.ragserver.db.CodeDatabase
import com.example.day.ragserver.db.toEmbeddingText
import com.example.day.ragserver.embedding.EmbeddingProvider
import com.example.day.ragserver.indexing.chunking.FixedSizeStrategy
import com.example.day.ragserver.indexing.chunking.LanguageAwareChunker
import com.example.day.ragserver.indexing.chunking.ast.KotlinTypeExtractor

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
            LanguageAwareChunker(useAst = config.useAstChunking),
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

        // Phase A: LLM extraction — один вызов на каждую top-level декларацию файла
        var totalProcessed = 0
        var totalErrors = 0
        for (file in ktFiles) {
            val content = try { file.readText() } catch (e: Exception) {
                println("  [WARN] Cannot read ${file.name}: ${e.message}")
                totalErrors++
                continue
            }
            val packageName = CodeMetadata.extractPackage(content)

            val declarations = db.getChunksByFile(file.absolutePath, "structural")
                .filter { it.nodeType in setOf("class_declaration", "object_declaration") && it.parentScope == null }
                .mapNotNull { it.declarationName }
                .distinct()
                .ifEmpty { listOf(file.nameWithoutExtension) }

            for (declaration in declarations) {
                if (!forceReindex && db.hasClassMetadata(declaration)) {
                    println("  [Metadata] Skipping '$declaration' — already indexed")
                    continue
                }
                val metadata = metadataExtractor!!.extract(content, declaration, packageName)
                if (metadata == null) { totalErrors++; continue }
                db.saveClassMetadata(metadata, file.absolutePath)
                totalProcessed++
            }
        }
        println("[Metadata] Phase A done — $totalProcessed extracted, $totalErrors errors")

        // Phase B: usedBy graph — AST-парсинг зависимостей, без LLM
        val knownClasses = db.getAllClassMetadata().map { it.className }.toSet()
        val dependencyMap = mutableMapOf<String, Set<String>>()

        for (file in ktFiles) {
            val content = try { file.readText() } catch (e: Exception) { continue }

            val rawTypes = KotlinTypeExtractor.extractReferencedTypes(content)
            val projectTypes = rawTypes.filter { it in knownClasses }.toSet()

            val fileDeclarations = db.getChunksByFile(file.absolutePath, "structural")
                .filter { it.nodeType in setOf("class_declaration", "object_declaration") && it.parentScope == null }
                .mapNotNull { it.declarationName }
                .distinct()
                .ifEmpty { listOf(file.nameWithoutExtension) }

            for (declaration in fileDeclarations) {
                dependencyMap[declaration] = projectTypes - setOf(declaration)
            }
        }

        // invert: dependencyMap[source] contains targets → usedByMap[target] += source
        val usedByMap = mutableMapOf<String, MutableList<String>>()
        for ((source, targets) in dependencyMap) {
            for (target in targets) {
                usedByMap.getOrPut(target) { mutableListOf() }.add(source)
            }
        }
        println("[Metadata] Phase B done — dependency graph built for ${dependencyMap.size} classes")

        // Phase C: enrich with usedBy + generate composite embeddings
        var embeddingErrors = 0
        for ((metadata, filePath) in db.getAllClassMetadataWithPaths()) {
            val enriched = metadata.copy(usedBy = usedByMap[metadata.className] ?: emptyList())
            db.saveClassMetadata(enriched, filePath)
            try {
                val vector = embeddingProvider.embed(enriched.toEmbeddingText())
                db.saveMetadataVector(enriched.className, vector)
            } catch (e: Exception) {
                println("  [WARN] Metadata vector failed for '${enriched.className}': ${e.message}")
                embeddingErrors++
            }
        }
        println("[Metadata] Phase C done — embeddings generated, $embeddingErrors errors")
    }
}
