package com.example.day.ragserver.indexing

import com.example.day.raggrammar.KotlinLanguage
import com.example.day.ragserver.db.ChunkEntity
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Node
import io.github.treesitter.ktreesitter.Parser
import java.time.Instant

interface ChunkingStrategy {
    val strategyName: String
    fun split(content: String, filePath: String, fileName: String): List<ChunkEntity>
}

// ──────────────────────────────────────────────────────────────────────────────
// Strategy A: Fixed-size sliding window with overlap
// ──────────────────────────────────────────────────────────────────────────────

class FixedSizeStrategy(
    val chunkSize: Int = 1000,
    val overlap: Int = 200,
) : ChunkingStrategy {

    override val strategyName = "fixed"

    override fun split(content: String, filePath: String, fileName: String): List<ChunkEntity> {
        if (content.isBlank()) return emptyList()

        val now = Instant.now().toString()
        val header = "// File: $fileName\n"
        val packageName = CodeMetadata.extractPackage(content)

        if (content.length <= chunkSize) {
            return listOf(
                ChunkEntity(
                    content = header + content,
                    filePath = filePath, fileName = fileName,
                    packageName = packageName, startLine = 1,
                    strategy = strategyName, chunkOrder = 0, indexedAt = now,
                )
            )
        }

        val chunks = mutableListOf<ChunkEntity>()
        val step = chunkSize - overlap
        var order = 0
        var charOffset = 0

        while (charOffset < content.length) {
            val end = minOf(charOffset + chunkSize, content.length)
            val startLine = 1 + content.substring(0, charOffset).count { it == '\n' }
            chunks.add(
                ChunkEntity(
                    content = header + content.substring(charOffset, end),
                    filePath = filePath, fileName = fileName,
                    packageName = packageName, startLine = startLine,
                    strategy = strategyName, chunkOrder = order++, indexedAt = now,
                )
            )
            if (end == content.length) break
            charOffset += step
        }

        return chunks
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Strategy B: Structural — split by Kotlin top-level declarations,
//             keeping KDoc comments and annotations attached to their declaration
// ──────────────────────────────────────────────────────────────────────────────

class StructuralStrategy(
    val maxChunkSize: Int = 2000,
) : ChunkingStrategy {

    override val strategyName = "structural"

    private val SPLIT_REGEX = Regex(
        """(?=\n(?:fun |class |interface |object |data class |sealed |enum |abstract |companion |typealias ))"""
    )

    override fun split(content: String, filePath: String, fileName: String): List<ChunkEntity> {
        if (content.isBlank()) return emptyList()

        val now = Instant.now().toString()
        val header = "// File: $fileName\n"
        val packageName = CodeMetadata.extractPackage(content)

        val rawBlocks = content.split(SPLIT_REGEX).filter { it.isNotBlank() }

        // Attach KDoc comments and annotations to the declaration that follows them
        val blocks = attachPreamblesForward(rawBlocks)

        // Line offsets computed from raw blocks (before preamble movement) give approximate
        // but stable file positions — sufficient for navigation.
        val lineOffsets = computeLineOffsets(rawBlocks)

        val chunks = mutableListOf<ChunkEntity>()
        var order = 0

        for ((index, block) in blocks.withIndex()) {
            val blockWithHeader = header + block.trim()
            val startLine = lineOffsets.getOrElse(index) { 1 }
            val declarationName = CodeMetadata.extractDeclarationName(block)

            if (blockWithHeader.length <= maxChunkSize) {
                chunks.add(
                    ChunkEntity(
                        content = blockWithHeader,
                        filePath = filePath, fileName = fileName,
                        packageName = packageName, declarationName = declarationName,
                        startLine = startLine,
                        strategy = strategyName, chunkOrder = order++, indexedAt = now,
                    )
                )
            } else {
                // Block too large — sub-split with FixedSize, preserving structural metadata
                val subChunks = FixedSizeStrategy(maxChunkSize, maxChunkSize / 5)
                    .split(block.trim(), filePath, fileName)
                subChunks.forEach { sub ->
                    chunks.add(
                        sub.copy(
                            strategy = strategyName,
                            packageName = packageName,
                            declarationName = declarationName,
                            startLine = startLine + (sub.startLine - 1),
                            chunkOrder = order++,
                            indexedAt = now,
                        )
                    )
                }
            }
        }

        return chunks
    }

    /**
     * Moves trailing KDoc comments and annotations from the end of each block
     * to the beginning of the next block, keeping declarations paired with their docs.
     *
     * Example — before:
     *   block[i]   = "fun foo() { ... }\n\n/** KDoc */\n@Inject"
     *   block[i+1] = "\nfun bar() { ... }"
     *
     * After:
     *   block[i]   = "fun foo() { ... }"
     *   block[i+1] = "/** KDoc */\n@Inject\nfun bar() { ... }"
     */
    private fun attachPreamblesForward(blocks: List<String>): List<String> {
        if (blocks.size <= 1) return blocks

        val result = ArrayList<String>(blocks.size)
        var pendingPreamble = ""

        for (block in blocks) {
            val full = pendingPreamble + block
            val (body, preamble) = extractTrailingPreamble(full)
            result.add(body)
            pendingPreamble = preamble
        }

        // The last block has nowhere to forward its preamble — keep it in place
        if (pendingPreamble.isNotBlank() && result.isNotEmpty()) {
            result[result.lastIndex] = result.last() + pendingPreamble
        }

        return result.filter { it.isNotBlank() }
    }

    /**
     * Splits a block into (mainBody, trailingPreamble).
     *
     * Trailing preamble = annotation lines (`@...`) and comment blocks (`/** */`, `//`)
     * that appear after the last "real" code line. These belong to the next declaration.
     * Trailing blank lines are kept with the main body, not the preamble.
     */
    private fun extractTrailingPreamble(block: String): Pair<String, String> {
        val lines = block.lines()

        // Find the last non-blank line — blank lines stay with the current block
        var lastNonBlank = lines.lastIndex
        while (lastNonBlank >= 0 && lines[lastNonBlank].trim().isEmpty()) lastNonBlank--

        // Walk backwards from last non-blank, collecting annotation and comment lines
        var preambleStart = lastNonBlank + 1
        var i = lastNonBlank
        while (i >= 0) {
            val trimmed = lines[i].trim()
            if (trimmed.startsWith("@") ||
                trimmed.startsWith("//") ||
                trimmed.startsWith("/*") ||
                trimmed.startsWith("*")
            ) {
                preambleStart = i
                i--
            } else {
                break
            }
        }

        if (preambleStart > lastNonBlank) return block to ""

        val main = lines.subList(0, preambleStart).joinToString("\n")
        val preamble = "\n" + lines.subList(preambleStart, lines.size).joinToString("\n")
        return main to preamble
    }

    /** Returns 1-based start line for each block, computed from cumulative newline counts. */
    private fun computeLineOffsets(rawBlocks: List<String>): List<Int> {
        val offsets = ArrayList<Int>(rawBlocks.size)
        var line = 1
        for (block in rawBlocks) {
            offsets.add(line)
            line += block.count { it == '\n' }
        }
        return offsets
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Strategy C: AST-based — split-then-merge via ktreesitter (Kotlin grammar)
// Корректно обрабатывает вложенность: companion object, лямбды, inner classes.
// ──────────────────────────────────────────────────────────────────────────────

private val AST_INTERESTING_TYPES = setOf(
    "class_declaration",
    "object_declaration",
    "companion_object",
    "function_declaration",
    "property_declaration",
    "typealias_declaration",
    "interface_declaration",
)

private data class AstChunk(
    val text: String,
    val startLine: Int,
    val declarationName: String?,
    val parentScope: String?,
)

class AstChunkingStrategy private constructor(
    private val parser: Parser,
    private val maxChunkSize: Int,
) : ChunkingStrategy {

    override val strategyName = "structural"

    companion object {
        fun create(maxChunkSize: Int = 2000): AstChunkingStrategy {
            val language = Language(KotlinLanguage.language())
            val parser = Parser(language)
            return AstChunkingStrategy(parser, maxChunkSize)
        }
    }

    override fun split(content: String, filePath: String, fileName: String): List<ChunkEntity> {
        if (content.isBlank()) return emptyList()

        val now = Instant.now().toString()
        val header = "// File: $fileName\n"
        val packageName = CodeMetadata.extractPackage(content)

        // Если файл целиком помещается — один чанк без разбора AST
        if (content.length <= maxChunkSize) {
            return listOf(
                ChunkEntity(
                    content = header + content,
                    filePath = filePath, fileName = fileName,
                    packageName = packageName, startLine = 1,
                    strategy = strategyName, chunkOrder = 0, indexedAt = now,
                )
            )
        }

        val candidates = extractCandidates(content)

        // Если AST ничего не нашёл (пустой файл, только imports) — fallback
        if (candidates.isEmpty()) {
            return FixedSizeStrategy(maxChunkSize, maxChunkSize / 5)
                .split(content, filePath, fileName)
                .map { it.copy(strategy = strategyName) }
        }

        val merged = mergeGreedy(candidates)
        val chunks = mutableListOf<ChunkEntity>()
        var order = 0

        for (candidate in merged) {
            val blockWithHeader = header + candidate.text
            if (blockWithHeader.length <= maxChunkSize) {
                chunks.add(
                    ChunkEntity(
                        content = blockWithHeader,
                        filePath = filePath, fileName = fileName,
                        packageName = packageName,
                        declarationName = candidate.declarationName,
                        parentScope = candidate.parentScope,
                        startLine = candidate.startLine,
                        strategy = strategyName, chunkOrder = order++, indexedAt = now,
                    )
                )
            } else {
                // Одна нода больше maxChunkSize — sub-split, сохраняем метаданные
                val subChunks = FixedSizeStrategy(maxChunkSize, maxChunkSize / 5)
                    .split(candidate.text, filePath, fileName)
                subChunks.forEach { sub ->
                    chunks.add(
                        sub.copy(
                            strategy = strategyName,
                            packageName = packageName,
                            declarationName = candidate.declarationName,
                            parentScope = candidate.parentScope,
                            startLine = candidate.startLine + (sub.startLine - 1),
                            chunkOrder = order++,
                            indexedAt = now,
                        )
                    )
                }
            }
        }

        return chunks
    }

    // Извлекаем «интересные» ноды AST верхнего уровня и ноды внутри классов (методы, props).
    // Companion object и вложенные классы — внутри родителя, не на top-level.
    private fun extractCandidates(source: String): List<AstChunk> {
        val tree = synchronized(parser) { parser.parse(source) }
        val root = tree.rootNode
        val result = mutableListOf<AstChunk>()
        collectNodes(source, root, parentName = null, result = result)
        return result
    }

    // Собирает все предшествующие комментарии (KDoc, однострочные //, многострочные /* */)
    // идущие подряд перед нодой. Возвращает текст комментариев (с переносом строки) и
    // первую строку (1-based) — чтобы скорректировать startLine чанка.
    private fun collectLeadingComments(node: Node): Pair<String, Int> {
        val comments = mutableListOf<String>()
        var firstLine = node.startPoint.row.toInt() + 1
        var sibling = node.prevNamedSibling
        while (sibling != null && sibling.type in setOf("multiline_comment", "line_comment")) {
            val commentText = sibling.text()?.toString() ?: break
            comments.add(0, commentText)
            firstLine = sibling.startPoint.row.toInt() + 1
            sibling = sibling.prevNamedSibling
        }
        return if (comments.isEmpty()) {
            "" to (node.startPoint.row.toInt() + 1)
        } else {
            (comments.joinToString("\n") + "\n") to firstLine
        }
    }

    private fun collectNodes(source: String, node: Node, parentName: String?, result: MutableList<AstChunk>) {
        for (child in node.children) {
            if (!child.isNamed) continue
            if (child.type in AST_INTERESTING_TYPES) {
                val declText = child.text()?.toString() ?: continue
                val name = child.childByFieldName("name")?.text()?.toString()
                val (leadingComments, startLine) = collectLeadingComments(child)
                val text = leadingComments + declText
                result.add(AstChunk(text, startLine, name, parentName))
                // Рекурсируем внутрь класса/объекта чтобы извлечь методы как отдельные чанки
                if (child.type in setOf("class_declaration", "object_declaration", "companion_object", "interface_declaration")) {
                    val body = child.childByFieldName("body")
                    if (body != null) collectNodes(source, body, name, result)
                }
            } else {
                collectNodes(source, child, parentName, result)
            }
        }
    }

    // Жадное слияние: если несколько соседних кандидатов суммарно < maxChunkSize — объединяем.
    // declarationName берётся от первого кандидата в группе.
    private fun mergeGreedy(candidates: List<AstChunk>): List<AstChunk> {
        if (candidates.isEmpty()) return emptyList()
        val result = mutableListOf<AstChunk>()
        var acc = candidates[0]

        for (i in 1 until candidates.size) {
            val next = candidates[i]
            // Не сливаем если разные parentScope (методы разных классов)
            if (acc.parentScope == next.parentScope && (acc.text.length + next.text.length + 1) <= maxChunkSize) {
                acc = acc.copy(text = acc.text + "\n\n" + next.text)
            } else {
                result.add(acc)
                acc = next
            }
        }
        result.add(acc)
        return result
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Strategy D: Markdown — split by headers (#, ##, ###)
// ──────────────────────────────────────────────────────────────────────────────

class MarkdownChunkingStrategy(
    val maxChunkSize: Int = 2000,
) : ChunkingStrategy {

    override val strategyName = "structural"

    private val HEADER_SPLIT_REGEX = Regex("""(?=\n#{1,3} )""")

    override fun split(content: String, filePath: String, fileName: String): List<ChunkEntity> {
        if (content.isBlank()) return emptyList()

        val now = Instant.now().toString()
        val header = "// File: $fileName\n"

        val sections = content.split(HEADER_SPLIT_REGEX).filter { it.isNotBlank() }

        if (sections.size <= 1) {
            // Нет заголовков — fallback на fixed
            return FixedSizeStrategy(maxChunkSize, maxChunkSize / 5)
                .split(content, filePath, fileName)
                .map { it.copy(strategy = strategyName) }
        }

        val chunks = mutableListOf<ChunkEntity>()
        var order = 0
        var currentLine = 1

        for (section in sections) {
            val declarationName = section.lines()
                .firstOrNull { it.startsWith("#") }
                ?.trimStart('#', ' ')
                ?.trim()

            val blockWithHeader = header + section.trim()
            val startLine = currentLine

            if (blockWithHeader.length <= maxChunkSize) {
                chunks.add(
                    ChunkEntity(
                        content = blockWithHeader,
                        filePath = filePath, fileName = fileName,
                        packageName = "", declarationName = declarationName,
                        startLine = startLine,
                        strategy = strategyName, chunkOrder = order++, indexedAt = now,
                    )
                )
            } else {
                val subChunks = FixedSizeStrategy(maxChunkSize, maxChunkSize / 5)
                    .split(section.trim(), filePath, fileName)
                subChunks.forEach { sub ->
                    chunks.add(
                        sub.copy(
                            strategy = strategyName,
                            declarationName = declarationName,
                            startLine = startLine + (sub.startLine - 1),
                            chunkOrder = order++,
                            indexedAt = now,
                        )
                    )
                }
            }

            currentLine += section.count { it == '\n' }
        }

        return chunks
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Strategy E: LanguageAwareChunker — routes by file extension + useAst flag.
// Implements ChunkingStrategy so IndexingService needs no changes.
// ──────────────────────────────────────────────────────────────────────────────

class LanguageAwareChunker(
    private val useAst: Boolean,
    private val maxChunkSize: Int = 2000,
) : ChunkingStrategy {

    override val strategyName = "structural"

    private val astStrategy by lazy { AstChunkingStrategy.create(maxChunkSize) }
    private val markdownStrategy = MarkdownChunkingStrategy(maxChunkSize)
    private val legacyStrategy = StructuralStrategy(maxChunkSize)
    private val fixedFallback = FixedSizeStrategy(maxChunkSize, maxChunkSize / 5)

    override fun split(content: String, filePath: String, fileName: String): List<ChunkEntity> {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when {
            ext in setOf("kt", "kts") && useAst -> astStrategy.split(content, filePath, fileName)
            ext in setOf("kt", "kts") -> legacyStrategy.split(content, filePath, fileName)
            ext == "md" -> markdownStrategy.split(content, filePath, fileName)
            else -> fixedFallback.split(content, filePath, fileName)
                .map { it.copy(strategy = strategyName) }
        }
    }
}
