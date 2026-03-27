package com.example.day.ragserver.indexing.chunking.ast

import com.example.day.raggrammar.KotlinLanguage
import com.example.day.ragserver.db.ChunkEntity
import com.example.day.ragserver.indexing.ChunkingStrategy
import com.example.day.ragserver.indexing.CodeMetadata
import com.example.day.ragserver.indexing.chunking.DEFAULT_MAX_CHUNK_SIZE
import com.example.day.ragserver.indexing.chunking.FixedSizeStrategy
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Node
import io.github.treesitter.ktreesitter.Parser
import java.time.Instant

// Strategy C: AST-based — split-then-merge via ktreesitter (Kotlin grammar)
// Корректно обрабатывает вложенность: companion object, лямбды, inner classes.

internal data class AstChunk(
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
        fun create(maxChunkSize: Int = DEFAULT_MAX_CHUNK_SIZE): AstChunkingStrategy {
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
            // AST-нода — атомарная семантическая единица: берём целиком, даже если > maxChunkSize.
            // FixedSize субсплит здесь бессмысленен: он режет посередине KDoc или сигнатуры.
            chunks.add(
                ChunkEntity(
                    content = header + candidate.text,
                    filePath = filePath, fileName = fileName,
                    packageName = packageName,
                    declarationName = candidate.declarationName,
                    parentScope = candidate.parentScope,
                    startLine = candidate.startLine,
                    strategy = strategyName, chunkOrder = order++, indexedAt = now,
                )
            )
        }

        return chunks
    }

    // node.text() uses byte offsets as char indices — breaks for non-ASCII source (Russian, etc.).
    // Always extract text via UTF-8 byte array to stay correct for any input.
    private fun nodeText(node: Node, sourceBytes: ByteArray): String {
        val start = node.startByte.toInt()
        val end = node.endByte.toInt().coerceAtMost(sourceBytes.size)
        if (start >= end) return ""
        return String(sourceBytes, start, end - start, Charsets.UTF_8)
    }

    // Извлекаем «интересные» ноды AST верхнего уровня и ноды внутри классов (методы, props).
    // Companion object и вложенные классы — внутри родителя, не на top-level.
    private fun extractCandidates(source: String): List<AstChunk> {
        val sourceBytes = source.toByteArray(Charsets.UTF_8)
        val tree = synchronized(parser) { parser.parse(source) }
        val root = tree.rootNode
        val result = mutableListOf<AstChunk>()
        collectNodes(sourceBytes, root, parentName = null, result = result)
        return result
    }

    // Собирает все предшествующие комментарии (KDoc, однострочные //, многострочные /* */)
    // идущие подряд перед нодой. Возвращает текст комментариев (с переносом строки) и
    // первую строку (1-based) — чтобы скорректировать startLine чанка.
    private fun collectLeadingComments(node: Node, sourceBytes: ByteArray): Pair<String, Int> {
        val comments = mutableListOf<String>()
        var firstLine = node.startPoint.row.toInt() + 1
        var sibling = node.prevNamedSibling
        while (sibling != null && sibling.type in AST_COMMENT_TYPES) {
            val commentText = nodeText(sibling, sourceBytes)
            if (commentText.isEmpty()) break
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

    private fun collectNodes(sourceBytes: ByteArray, node: Node, parentName: String?, result: MutableList<AstChunk>) {
        for (child in node.namedChildren) {
            if (!child.isNamed) continue
            if (child.type in AST_INTERESTING_TYPES) {
                val name = extractDeclarationName(child, sourceBytes)
                val (leadingComments, startLine) = collectLeadingComments(child, sourceBytes)

                val declText = if (child.type in AST_CONTAINER_TYPES) {
                    // Для контейнеров берём только заголовок (сигнатуру до тела).
                    // Содержимое тела извлекается рекурсией как отдельные чанки.
                    val body = child.namedChildren.firstOrNull { it.type in AST_BODY_TYPES }
                    if (body != null) {
                        val start = child.startByte.toInt()
                        val end = body.startByte.toInt()
                        if (start < end) String(sourceBytes, start, end - start, Charsets.UTF_8).trimEnd()
                        else nodeText(child, sourceBytes)
                    } else nodeText(child, sourceBytes)
                } else {
                    nodeText(child, sourceBytes)
                }

                if (declText.isEmpty()) continue
                result.add(AstChunk(leadingComments + declText, startLine, name, parentName))

                if (child.type in AST_CONTAINER_TYPES) {
                    val body = child.namedChildren.firstOrNull { it.type in AST_BODY_TYPES }
                    // companion object без имени: пробрасываем имя родителя, чтобы не терять parentScope
                    val scopeName = name ?: parentName
                    if (body != null) collectNodes(sourceBytes, body, scopeName, result)
                }
            } else {
                collectNodes(sourceBytes, child, parentName, result)
            }
        }
    }

    private fun extractDeclarationName(node: Node, sourceBytes: ByteArray): String? {
        // field "name" — работает для class_declaration, function_declaration, object_declaration, type_alias
        node.childByFieldName("name")?.let {
            return nodeText(it, sourceBytes).ifBlank { null }
        }

        // property_declaration: имя лежит внутри variable_declaration → simple_identifier
        // val version = "1.0"  →  property_declaration > variable_declaration > simple_identifier
        if (node.type == "property_declaration") {
            node.namedChildren.firstOrNull { it.type == "variable_declaration" }?.let { varDecl ->
                val nameNode = varDecl.childByFieldName("name")
                    ?: varDecl.namedChildren.firstOrNull { it.type == "simple_identifier" }
                nameNode?.let { return nodeText(it, sourceBytes).ifBlank { null } }
            }
        }

        // type_identifier — используется в некоторых узлах как имя типа
        node.namedChildren.firstOrNull { it.type == "type_identifier" }?.let {
            return nodeText(it, sourceBytes).ifBlank { null }
        }

        // Последний шанс — ищем simple_identifier среди прямых детей
        node.namedChildren.firstOrNull {
            it.type in setOf("simple_identifier", "identifier", "object_identifier")
        }?.let {
            return nodeText(it, sourceBytes).ifBlank { null }
        }

        return null
    }

    // Жадное слияние: если несколько соседних кандидатов суммарно < maxChunkSize — объединяем.
    // declarationName берётся от первого кандидата в группе.
    private fun mergeGreedy(candidates: List<AstChunk>): List<AstChunk> {
        if (candidates.isEmpty()) return emptyList()
        val result = mutableListOf<AstChunk>()
        var acc = candidates[0]

        for (i in 1 until candidates.size) {
            val next = candidates[i]
            // Сливаем только соседей с одинаковым parentScope и суммарным размером ≤ maxChunkSize.
            // declarationName первого кандидата в группе остаётся у merged-чанка.
            if (acc.parentScope == next.parentScope &&
                (acc.text.length + next.text.length + 1) <= maxChunkSize
            ) {
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
