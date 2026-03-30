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

        if (content.length <= maxChunkSize) {
            return listOf(
                ChunkEntity(
                    content = header + content,
                    filePath = filePath,
                    fileName = fileName,
                    packageName = packageName,
                    startLine = 1,
                    strategy = strategyName,
                    chunkOrder = 0,
                    indexedAt = now,
                )
            )
        }

        val candidates = extractCandidates(content)
        if (candidates.isEmpty()) {
            return FixedSizeStrategy(maxChunkSize, maxChunkSize / 5)
                .split(content, filePath, fileName)
                .map { it.copy(strategy = strategyName) }
        }

        val merged = mergeGreedy(candidates)
        return merged.mapIndexed { order, candidate ->
            ChunkEntity(
                content = header + candidate.text,
                filePath = filePath,
                fileName = fileName,
                packageName = packageName,
                declarationName = candidate.declarationName,
                parentScope = candidate.parentScope,
                startLine = candidate.startLine,
                strategy = strategyName,
                chunkOrder = order,
                indexedAt = now,
            )
        }
    }

    // Node byte offsets must be decoded from UTF-8 bytes to keep non-ASCII source correct.
    private fun nodeText(node: Node, sourceBytes: ByteArray): String {
        val start = node.startByte.toInt()
        val end = node.endByte.toInt().coerceAtMost(sourceBytes.size)
        if (start >= end) return ""
        return String(sourceBytes, start, end - start, Charsets.UTF_8)
    }

    private fun extractCandidates(source: String): List<AstChunk> {
        val sourceBytes = source.toByteArray(Charsets.UTF_8)
        val tree = synchronized(parser) { parser.parse(source) }
        val root = tree.rootNode
        val result = mutableListOf<AstChunk>()
        collectNodes(sourceBytes, root, parentName = null, result = result)
        return result
    }

    private fun collectLeadingComments(node: Node, sourceBytes: ByteArray): Pair<String, Int> {
        val comments = mutableListOf<String>()
        var firstLine = node.startPoint.row.toInt() + 1
        var sibling = node.prevNamedSibling

        while (sibling != null) {
            when {
                sibling.type in AST_COMMENT_TYPES -> {
                    val commentText = nodeText(sibling, sourceBytes)
                    if (commentText.isEmpty()) break
                    comments.add(0, commentText)
                    firstLine = sibling.startPoint.row.toInt() + 1
                    sibling = sibling.prevNamedSibling
                }

                sibling.type in AST_COMMENT_BRIDGE_TYPES -> {
                    sibling = sibling.prevNamedSibling
                }

                else -> break
            }
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

            if (child.type !in AST_INTERESTING_TYPES) {
                collectNodes(sourceBytes, child, parentName, result)
                continue
            }

            val name = extractDeclarationName(child, sourceBytes)
            val declarationStartLine = child.startPoint.row.toInt() + 1
            val (leadingComments, commentsStartLine) = collectLeadingComments(child, sourceBytes)

            if (child.type == "function_declaration") {
                result.addAll(
                    collectFunctionChunks(
                        functionNode = child,
                        sourceBytes = sourceBytes,
                        functionName = name,
                        parentName = parentName,
                        declarationStartLine = declarationStartLine,
                        leadingComments = leadingComments,
                        commentsStartLine = commentsStartLine,
                    )
                )
                continue
            }

            val declarationText = declarationHeaderText(child, sourceBytes)
            if (declarationText.isNotBlank()) {
                addDeclarationChunks(
                    sink = result,
                    declarationText = declarationText,
                    declarationName = name,
                    parentScope = parentName,
                    declarationStartLine = declarationStartLine,
                    leadingComments = leadingComments,
                    commentsStartLine = commentsStartLine,
                )
            }

            if (child.type in AST_CONTAINER_TYPES) {
                val body = child.namedChildren.firstOrNull { it.type in AST_BODY_TYPES }
                val scopeName = name ?: parentName
                if (body != null) {
                    collectNodes(sourceBytes, body, scopeName, result)
                }
            }
        }
    }

    private fun collectFunctionChunks(
        functionNode: Node,
        sourceBytes: ByteArray,
        functionName: String?,
        parentName: String?,
        declarationStartLine: Int,
        leadingComments: String,
        commentsStartLine: Int,
    ): List<AstChunk> {
        val fullFunctionText = nodeText(functionNode, sourceBytes).trimEnd()
        if (fullFunctionText.isBlank()) return emptyList()

        val functionBody = resolveFunctionBody(functionNode, sourceBytes)
        if (functionBody == null || fullFunctionText.length <= maxChunkSize) {
            val chunks = mutableListOf<AstChunk>()
            addDeclarationChunks(
                sink = chunks,
                declarationText = fullFunctionText,
                declarationName = functionName,
                parentScope = parentName,
                declarationStartLine = declarationStartLine,
                leadingComments = leadingComments,
                commentsStartLine = commentsStartLine,
            )
            return chunks
        }

        val signatureStart = functionNode.startByte.toInt()
        val signatureEnd = functionBody.startByte.toInt().coerceAtMost(sourceBytes.size)
        val signatureText = if (signatureStart < signatureEnd) {
            String(sourceBytes, signatureStart, signatureEnd - signatureStart, Charsets.UTF_8).trimEnd()
        } else {
            fullFunctionText
        }

        val chunks = mutableListOf<AstChunk>()
        addDeclarationChunks(
            sink = chunks,
            declarationText = signatureText,
            declarationName = functionName,
            parentScope = parentName,
            declarationStartLine = declarationStartLine,
            leadingComments = leadingComments,
            commentsStartLine = commentsStartLine,
        )

        chunks.addAll(
            splitFunctionBody(
                bodyNode = functionBody,
                sourceBytes = sourceBytes,
                functionName = functionName,
                parentName = parentName,
            )
        )

        return chunks
    }

    private fun resolveFunctionBody(functionNode: Node, sourceBytes: ByteArray): Node? {
        functionNode.childByFieldName("body")?.let { return it }

        // tree-sitter-kotlin occasionally omits "body" field access in bindings.
        // Fallback to the last named child only if it syntactically looks like a body.
        val candidate = functionNode.namedChildren.lastOrNull() ?: return null
        val text = nodeText(candidate, sourceBytes).trimStart()
        return if (text.startsWith("{") || text.startsWith("=")) candidate else null
    }

    private fun splitFunctionBody(
        bodyNode: Node,
        sourceBytes: ByteArray,
        functionName: String?,
        parentName: String?,
    ): List<AstChunk> {
        val bodyChildren = bodyNode.namedChildren.filter { it.isNamed }
        if (bodyChildren.isEmpty()) {
            val bodyText = nodeText(bodyNode, sourceBytes).trim()
            if (bodyText.isBlank()) return emptyList()
            return listOf(
                AstChunk(
                    text = bodyText,
                    startLine = bodyNode.startPoint.row.toInt() + 1,
                    declarationName = functionName?.let { "$it#part1" },
                    parentScope = parentName,
                )
            )
        }

        val chunks = mutableListOf<AstChunk>()
        val buffer = StringBuilder()
        var partStartLine = bodyChildren.first().startPoint.row.toInt() + 1
        var partIndex = 1

        for (child in bodyChildren) {
            val piece = nodeText(child, sourceBytes).trim()
            if (piece.isBlank()) continue

            val projectedLength = if (buffer.isEmpty()) {
                piece.length
            } else {
                buffer.length + 2 + piece.length
            }

            if (buffer.isNotEmpty() && projectedLength > maxChunkSize) {
                chunks.add(
                    AstChunk(
                        text = buffer.toString().trimEnd(),
                        startLine = partStartLine,
                        declarationName = functionName?.let { "$it#part$partIndex" },
                        parentScope = parentName,
                    )
                )
                partIndex += 1
                buffer.clear()
                partStartLine = child.startPoint.row.toInt() + 1
            }

            if (buffer.isNotEmpty()) buffer.append("\n\n")
            buffer.append(piece)
        }

        if (buffer.isNotEmpty()) {
            chunks.add(
                AstChunk(
                    text = buffer.toString().trimEnd(),
                    startLine = partStartLine,
                    declarationName = functionName?.let { "$it#part$partIndex" },
                    parentScope = parentName,
                )
            )
        }

        return chunks
    }

    private fun addDeclarationChunks(
        sink: MutableList<AstChunk>,
        declarationText: String,
        declarationName: String?,
        parentScope: String?,
        declarationStartLine: Int,
        leadingComments: String,
        commentsStartLine: Int,
    ) {
        val cleanDeclaration = declarationText.trimEnd()
        if (cleanDeclaration.isBlank()) return

        if (leadingComments.isNotBlank() && (leadingComments.length + cleanDeclaration.length) > maxChunkSize) {
            sink.add(
                AstChunk(
                    text = leadingComments.trimEnd(),
                    startLine = commentsStartLine,
                    declarationName = declarationName,
                    parentScope = parentScope,
                )
            )
            sink.add(
                AstChunk(
                    text = cleanDeclaration,
                    startLine = declarationStartLine,
                    declarationName = declarationName,
                    parentScope = parentScope,
                )
            )
            return
        }

        val text = (leadingComments + cleanDeclaration).trimEnd()
        val startLine = if (leadingComments.isNotBlank()) commentsStartLine else declarationStartLine
        sink.add(
            AstChunk(
                text = text,
                startLine = startLine,
                declarationName = declarationName,
                parentScope = parentScope,
            )
        )
    }

    private fun declarationHeaderText(node: Node, sourceBytes: ByteArray): String {
        if (node.type !in AST_CONTAINER_TYPES) return nodeText(node, sourceBytes).trimEnd()

        val body = node.namedChildren.firstOrNull { it.type in AST_BODY_TYPES }
        if (body == null) return nodeText(node, sourceBytes).trimEnd()

        val start = node.startByte.toInt()
        val end = body.startByte.toInt().coerceAtMost(sourceBytes.size)
        if (start >= end) return nodeText(node, sourceBytes).trimEnd()
        return String(sourceBytes, start, end - start, Charsets.UTF_8).trimEnd()
    }

    private fun extractDeclarationName(node: Node, sourceBytes: ByteArray): String? {
        node.childByFieldName("name")?.let {
            return nodeText(it, sourceBytes).ifBlank { null }
        }

        if (node.type == "property_declaration") {
            node.namedChildren.firstOrNull { it.type == "variable_declaration" }?.let { varDecl ->
                val nameNode = varDecl.childByFieldName("name")
                    ?: varDecl.namedChildren.firstOrNull { it.type == "simple_identifier" }
                nameNode?.let { return nodeText(it, sourceBytes).ifBlank { null } }
            }
        }

        node.namedChildren.firstOrNull { it.type == "type_identifier" }?.let {
            return nodeText(it, sourceBytes).ifBlank { null }
        }

        node.namedChildren.firstOrNull {
            it.type in setOf("simple_identifier", "identifier", "object_identifier")
        }?.let {
            return nodeText(it, sourceBytes).ifBlank { null }
        }

        return null
    }

    private fun mergeGreedy(candidates: List<AstChunk>): List<AstChunk> {
        if (candidates.isEmpty()) return emptyList()

        val result = mutableListOf<AstChunk>()
        var acc = candidates[0]

        for (i in 1 until candidates.size) {
            val next = candidates[i]
            if (acc.parentScope == next.parentScope &&
                acc.declarationName != null &&
                acc.declarationName == next.declarationName &&
                (acc.text.length + next.text.length + 2) <= maxChunkSize
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
