package com.example.day.ragserver.indexing.chunking.ast

import com.example.day.ragserver.db.ChunkEntity
import com.example.day.ragserver.indexing.ChunkingStrategy
import com.example.day.ragserver.indexing.CodeMetadata
import com.example.day.ragserver.indexing.chunking.DEFAULT_MAX_CHUNK_SIZE
import com.example.day.ragserver.indexing.chunking.FixedSizeStrategy
import io.github.treesitter.ktreesitter.Node
import java.time.Instant

/**
 * Промежуточное представление одного чанка до его превращения в [ChunkEntity].
 *
 * Используется внутри стратегии для накопления и слияния фрагментов кода перед финальной
 * сборкой [ChunkEntity] в [AstChunkingStrategy.split].
 *
 * @property text       Исходный текст чанка (без файловых заголовков — они добавляются позже).
 * @property startLine  Номер строки в файле, с которой начинается чанк (1-based).
 * @property declarationName  Имя декларации (класс, функция, свойство). null если чанк является
 *                            результатом слияния нескольких деклараций с разными именами.
 * @property parentScope  Имя контейнера, внутри которого находится декларация (класс или объект).
 *                        null для деклараций верхнего уровня.
 * @property isFun    true если чанк представляет функцию. Управляет отображением имени в [buildContextPath]:
 *                    целые функции не добавляют своё имя в контекстный путь, только разбитые части.
 * @property isSplit  true если чанк является частью функции или companion, разбитой через [splitLargeDeclaration].
 *                    Предотвращает слияние с соседними декларациями в [mergeGreedy].
 * @property nodeType  Тип AST-узла из tree-sitter grammar: "class_declaration", "object_declaration",
 *                     "function_declaration", "property_declaration", "type_alias", "companion_object".
 *                     null для синтетических чанков (merged, split parts, fixed fallback).
 */
internal data class AstChunk(
    val text: String,
    val startLine: Int,
    val declarationName: String?,
    val parentScope: String?,
    val isFun: Boolean = false,
    val isSplit: Boolean = false,
    val nodeType: String? = null,
)

class AstChunkingStrategy private constructor(
    private val maxChunkSize: Int,
) : ChunkingStrategy {

    override val strategyName = "structural"

    companion object {
        /**
         * Создаёт экземпляр стратегии. Парсинг делегируется [KotlinAstParser].
         *
         * @param maxChunkSize  Максимальный размер одного чанка в символах (не байтах).
         *                      По умолчанию [DEFAULT_MAX_CHUNK_SIZE] = 2000.
         */
        fun create(maxChunkSize: Int = DEFAULT_MAX_CHUNK_SIZE): AstChunkingStrategy {
            return AstChunkingStrategy(maxChunkSize)
        }
    }

    /**
     * Разбивает исходный код Kotlin-файла на семантические чанки для RAG-индексации.
     *
     * Алгоритм:
     * 1. Если файл целиком помещается в [maxChunkSize] — возвращает один чанк.
     * 2. Иначе парсит AST через tree-sitter и извлекает кандидатов через [extractCandidates].
     * 3. Если AST не дал кандидатов (нет известных деклараций) — падает на [FixedSizeStrategy].
     * 4. Объединяет соседние мелкие чанки одного уровня через [mergeGreedy].
     * 5. Каждому чанку добавляет заголовки `// File:` и `// Context:` для LLM-контекста.
     *
     * `// Context:` строится из `packageName > parentScope > declarationName` и позволяет
     * LLM понять, какому классу/объекту принадлежит каждый фрагмент кода.
     */
    override fun split(content: String, filePath: String, fileName: String): List<ChunkEntity> {
        if (content.isBlank()) return emptyList()

        val now = Instant.now().toString()
        val fileHeader = "// File: $fileName\n"
        val packageName = CodeMetadata.extractPackage(content)

        if (content.length <= maxChunkSize) {
            return listOf(
                ChunkEntity(
                    content = fileHeader + content,
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
            val contextPath = buildContextPath(packageName, candidate.parentScope, candidate.declarationName, candidate.isFun, candidate.isSplit)
            val contextLine = if (contextPath.isNotEmpty()) "// Context: $contextPath\n" else ""
            ChunkEntity(
                content = fileHeader + contextLine + candidate.text,
                filePath = filePath,
                fileName = fileName,
                packageName = packageName,
                declarationName = candidate.declarationName,
                parentScope = candidate.parentScope,
                contextPath = contextPath.ifEmpty { null },
                startLine = candidate.startLine,
                nodeType = candidate.nodeType,
                strategy = strategyName,
                chunkOrder = order,
                indexedAt = now,
            )
        }
    }

    /**
     * Строит хлебную крошку контекста для строки `// Context:` в содержимом чанка.
     *
     * Формат: `packageName > parentScope > declarationName`, где каждая часть добавляется
     * только если она непустая (null-части пропускаются через [listOfNotNull]).
     *
     * Логика включения имени декларации:
     * - Обычные декларации (класс, свойство, typealias) — включаются всегда.
     * - Целые функции ([isFun]=true, [isSplit]=false) — имя НЕ включается. Контекст заканчивается
     *   на классе-владельце, чтобы не вводить в заблуждение при слиянии нескольких функций в один чанк.
     * - Разбитые функции ([isFun]=true, [isSplit]=true) — включается `functionName() #partN`,
     *   где `#partN` извлекается из [declarationName] (формат: `"prepareContext#part2"`).
     */
    private fun buildContextPath(
        packageName: String,
        parentScope: String?,
        declarationName: String?,
        isFun: Boolean = false,
        isSplit: Boolean = false,
    ): String {
        val declPart = when {
            declarationName == null -> null
            isFun && !isSplit -> null  // whole function: context ends at class scope
            isFun && isSplit -> "${declarationName.substringBefore("#")}() ${declarationName.substringAfter("#")}"  // prepareContext() #part2
            else -> declarationName
        }
        return listOfNotNull(packageName.ifEmpty { null }, parentScope, declPart).joinToString(" > ")
    }

    /**
     * Парсит исходный код через tree-sitter и собирает все чанки-кандидаты из AST.
     *
     * Парсер не является потокобезопасным, поэтому вызов [parser.parse] защищён [synchronized].
     * После получения дерева рекурсивный обход выполняется через [collectNodes].
     *
     * Возвращает пустой список, если в файле не обнаружено ни одной известной декларации
     * (например, файл содержит только import-ы или пустой package). В этом случае
     * [split] переключается на [FixedSizeStrategy].
     */
    private fun extractCandidates(source: String): List<AstChunk> {
        val (root, sourceBytes) = KotlinAstParser.parse(source)
        val result = mutableListOf<AstChunk>()
        collectNodes(sourceBytes, root, parentName = null, result = result, skipPropertyDeclarations = false)
        return result
    }

    /**
     * Собирает блок ведущих комментариев непосредственно перед [node].
     *
     * Алгоритм идёт назад по предыдущим именованным сиблингам:
     * - Если сиблинг — комментарий ([AST_COMMENT_TYPES]): добавляет в начало списка,
     *   продолжает идти назад.
     * - Если сиблинг — «мост» ([AST_COMMENT_BRIDGE_TYPES], например аннотация или модификатор):
     *   пропускает его и продолжает идти назад (комментарий может быть отделён аннотацией).
     * - Иначе: останавливается.
     *
     * Возвращает пару: (текст комментариев с `\n` в конце, номер первой строки блока).
     * Если комментариев нет — возвращает ("", startLine самого узла).
     */
    private fun collectLeadingComments(node: Node, sourceBytes: ByteArray): Pair<String, Int> {
        val comments = mutableListOf<String>()
        var firstLine = node.startPoint.row.toInt() + 1
        var sibling = node.prevNamedSibling

        while (sibling != null) {
            when {
                sibling.type in AST_COMMENT_TYPES -> {
                    val commentText = KotlinAstParser.nodeText(sibling, sourceBytes)
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

    /**
     * Рекурсивно обходит дочерние узлы [node] и добавляет чанки в [result].
     *
     * Логика обработки каждого дочернего узла:
     * - **property_declaration** при [skipPropertyDeclarations]=true — пропускается.
     *   Свойства внутри тела класса уже собраны в [buildContainerChunk].
     * - **companion_object** — всегда пропускается: companion собирается целиком
     *   через [collectCompanionChunks] внутри [buildContainerChunk].
     * - **не из [AST_INTERESTING_TYPES]** — рекурсивно заходим внутрь (например, в
     *   `when`-выражение может быть вложенная функция).
     * - **function_declaration** — делегируется в [collectFunctionChunks].
     * - **контейнер** ([AST_CONTAINER_TYPES], т.е. класс или объект):
     *     - Маленький (весь текст ≤ [maxChunkSize]) → один чанк с полным телом, рекурсия не нужна.
     *     - Большой → [buildContainerChunk] для заголовка+свойств, затем рекурсия в тело
     *       с [skipPropertyDeclarations]=true (свойства уже обработаны).
     * - **всё остальное** (top-level свойство, type_alias) → [addDeclarationChunks].
     *
     * @param parentName  Имя класса/объекта-владельца, или null для деклараций верхнего уровня.
     * @param skipPropertyDeclarations  true при рекурсии внутрь тела класса — свойства уже в классовом чанке.
     */
    private fun collectNodes(
        sourceBytes: ByteArray,
        node: Node,
        parentName: String?,
        result: MutableList<AstChunk>,
        skipPropertyDeclarations: Boolean,
    ) {
        for (child in node.namedChildren) {
            if (!child.isNamed) continue

            // Properties inside a container body are handled by buildContainerChunk — skip here.
            if (skipPropertyDeclarations && child.type == "property_declaration") continue

            // companion_object is always collected as a whole unit in buildContainerChunk — never recurse into it.
            if (child.type == "companion_object") continue

            if (child.type !in AST_INTERESTING_TYPES) {
                collectNodes(sourceBytes, child, parentName, result, skipPropertyDeclarations)
                continue
            }

            val name = extractDeclarationName(child, sourceBytes)
            val declarationStartLine = child.startPoint.row.toInt() + 1
            val (leadingComments, commentsStartLine) = collectLeadingComments(child, sourceBytes)

            val childNodeType = child.type

            if (childNodeType == "function_declaration") {
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

            if (childNodeType in AST_CONTAINER_TYPES) {
                val entireText = (leadingComments + KotlinAstParser.nodeText(child, sourceBytes)).trimEnd()
                val startLine = if (leadingComments.isNotBlank()) commentsStartLine else declarationStartLine

                if (entireText.length <= maxChunkSize) {
                    // Small container: emit the entire class text as one chunk.
                    // Methods, properties, and companion are all included — no recursion needed.
                    result.add(AstChunk(text = entireText, startLine = startLine, declarationName = name, parentScope = parentName, nodeType = childNodeType))
                } else {
                    // Large container: header+props chunk via buildContainerChunk, then recurse for methods.
                    buildContainerChunk(
                        node = child,
                        sourceBytes = sourceBytes,
                        name = name,
                        parentName = parentName,
                        declarationStartLine = declarationStartLine,
                        leadingComments = leadingComments,
                        commentsStartLine = commentsStartLine,
                        nodeType = childNodeType,
                        result = result,
                    )
                    val body = child.namedChildren.firstOrNull { it.type in AST_BODY_TYPES }
                    val scopeName = name ?: parentName
                    if (body != null) {
                        // Recurse for methods and nested types; properties were already collected above.
                        collectNodes(sourceBytes, body, scopeName, result, skipPropertyDeclarations = true)
                    }
                }
                continue
            }

            // Top-level property_declaration, type_alias, and other interesting types.
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
                    nodeType = childNodeType,
                )
            }
        }
    }

    /**
     * Builds chunk(s) for a container declaration (class/object_declaration).
     *
     * Tries to produce the fewest chunks possible, in priority order:
     *   1. header + properties + companion → single class chunk (everything together)
     *   2. header + properties → class chunk, companion separately
     *   3. header alone → class chunk, property chunks separately, companion separately
     *
     * companion object is collected as a whole (see [collectCompanionChunks]).
     */
    private fun buildContainerChunk(
        node: Node,
        sourceBytes: ByteArray,
        name: String?,
        parentName: String?,
        declarationStartLine: Int,
        leadingComments: String,
        commentsStartLine: Int,
        nodeType: String,
        result: MutableList<AstChunk>,
    ) {
        val headerText = declarationHeaderText(node, sourceBytes)
        if (headerText.isBlank()) return

        val startLine = if (leadingComments.isNotBlank()) commentsStartLine else declarationStartLine
        val body = node.namedChildren.firstOrNull { it.type in AST_BODY_TYPES }

        val propertyChunks = if (body != null) collectPropertyChunks(body, sourceBytes, name ?: parentName) else emptyList()
        val companionChunks = if (body != null) collectCompanionChunks(body, sourceBytes, name ?: parentName) else emptyList()

        val propertiesText = propertyChunks.joinToString("\n") { it.text }
        // Only attempt to inline companion when it fits as a single unsplit chunk.
        val companionInlineText = companionChunks.takeIf { it.size == 1 && !it[0].isSplit }?.get(0)?.text ?: ""

        // Phase 1: try header + properties + companion → one class chunk.
        val hasBodyContent = propertiesText.isNotBlank() || companionInlineText.isNotBlank()
        val fullCombined = buildString {
            if (leadingComments.isNotBlank()) append(leadingComments)
            append(headerText)
            if (hasBodyContent) {
                append(" {")
                if (propertiesText.isNotBlank()) { append("\n"); append(propertiesText) }
                if (companionInlineText.isNotBlank()) { append("\n"); append(companionInlineText) }
                append("\n}")
            }
        }.trimEnd()

        if (fullCombined.length <= maxChunkSize) {
            result.add(AstChunk(text = fullCombined, startLine = startLine, declarationName = name, parentScope = parentName, nodeType = nodeType))
            if (companionInlineText.isNotBlank()) return  // companion was inlined — nothing more to emit
            // Companion was too large to inline — fall through to emit its chunks separately.
            result.addAll(companionChunks)
            return
        }

        // Phase 2/3: emit class chunk (with or without properties), companion separately.
        val classText = buildString {
            if (leadingComments.isNotBlank()) append(leadingComments)
            append(headerText)
            if (propertiesText.isNotBlank()) {
                append(" {")
                append("\n")
                append(propertiesText)
                append("\n}")
            }
        }.trimEnd()

        if (classText.length <= maxChunkSize) {
            result.add(AstChunk(text = classText, startLine = startLine, declarationName = name, parentScope = parentName, nodeType = nodeType))
        } else {
            // Phase 3: header alone; properties go as individual chunks (mergeGreedy consolidates).
            val headerOnly = (leadingComments + headerText).trimEnd()
            result.add(AstChunk(text = headerOnly, startLine = startLine, declarationName = name, parentScope = parentName, nodeType = nodeType))
            result.addAll(propertyChunks)
        }

        result.addAll(companionChunks)
    }

    /**
     * Collects companion object(s) from a class body as whole-unit chunks.
     * Each companion is emitted as one chunk (leading comments + full `companion object { … }` text).
     * If the companion exceeds [maxChunkSize] it is split via [splitLargeDeclaration].
     *
     * Context path: `pkg > ClassName > companion object` (or `companion object Name` if named).
     */
    private fun collectCompanionChunks(body: Node, sourceBytes: ByteArray, parentScope: String?): List<AstChunk> {
        val result = mutableListOf<AstChunk>()
        for (child in body.namedChildren) {
            if (!child.isNamed || child.type != "companion_object") continue
            val (comments, commentsLine) = collectLeadingComments(child, sourceBytes)
            val fullText = (comments + KotlinAstParser.nodeText(child, sourceBytes)).trimEnd()
            if (fullText.isBlank()) continue
            val startLine = if (comments.isNotBlank()) commentsLine else child.startPoint.row.toInt() + 1
            val extractedName = extractDeclarationName(child, sourceBytes)
            val companionLabel = if (extractedName != null) "companion object $extractedName" else "companion object"
            if (fullText.length <= maxChunkSize) {
                result.add(AstChunk(text = fullText, startLine = startLine, declarationName = companionLabel, parentScope = parentScope))
            } else {
                result.addAll(splitLargeDeclaration(text = fullText, baseStartLine = startLine, declarationName = companionLabel, parentScope = parentScope))
            }
        }
        return result
    }

    /**
     * Collects all [property_declaration] nodes from a class/object body,
     * each with its leading comments, as independent [AstChunk]s.
     * Used by [buildContainerChunk] when properties don't fit into the class chunk.
     */
    private fun collectPropertyChunks(body: Node, sourceBytes: ByteArray, parentScope: String?): List<AstChunk> {
        val chunks = mutableListOf<AstChunk>()
        for (child in body.namedChildren) {
            if (!child.isNamed || child.type != "property_declaration") continue
            val propText = KotlinAstParser.nodeText(child, sourceBytes).trimEnd()
            if (propText.isBlank()) continue
            val propLine = child.startPoint.row.toInt() + 1
            val (comments, commentsLine) = collectLeadingComments(child, sourceBytes)
            val fullText = (comments + propText).trimEnd()
            val startLine = if (comments.isNotBlank()) commentsLine else propLine
            chunks.add(
                AstChunk(
                    text = fullText,
                    startLine = startLine,
                    declarationName = extractDeclarationName(child, sourceBytes),
                    parentScope = parentScope,
                )
            )
        }
        return chunks
    }

    /**
     * Produces chunks for a function declaration.
     *
     * Strategy:
     * - If the full function text (leading comments + signature + body) fits in [maxChunkSize],
     *   it is emitted as a single chunk — keeping KDoc, signature, and body together.
     * - Otherwise the full text is split using a fixed-size strategy with overlap so every
     *   part retains context via the `// Context:` header added in [split].
     */
    private fun collectFunctionChunks(
        functionNode: Node,
        sourceBytes: ByteArray,
        functionName: String?,
        parentName: String?,
        declarationStartLine: Int,
        leadingComments: String,
        commentsStartLine: Int,
    ): List<AstChunk> {
        val functionText = KotlinAstParser.nodeText(functionNode, sourceBytes).trimEnd()
        if (functionText.isBlank()) return emptyList()

        val fullText = (leadingComments + functionText).trimEnd()
        val startLine = if (leadingComments.isNotBlank()) commentsStartLine else declarationStartLine

        if (fullText.length <= maxChunkSize) {
            return listOf(
                AstChunk(
                    text = fullText,
                    startLine = startLine,
                    declarationName = functionName,
                    parentScope = parentName,
                    isFun = true,
                    nodeType = "function_declaration",
                )
            )
        }

        return splitLargeDeclaration(
            text = fullText,
            baseStartLine = startLine,
            declarationName = functionName,
            parentScope = parentName,
            isFun = true,
        )
    }

    /**
     * Splits a declaration text that exceeds [maxChunkSize] into fixed-size parts with overlap.
     * Each part carries the same [declarationName] suffixed with `#part1`, `#part2`, etc.,
     * so the `// Context:` header in [split] gives every part its full scope path.
     */
    private fun splitLargeDeclaration(
        text: String,
        baseStartLine: Int,
        declarationName: String?,
        parentScope: String?,
        isFun: Boolean = false,
    ): List<AstChunk> {
        val overlap = maxChunkSize / 5
        val result = mutableListOf<AstChunk>()
        var offset = 0
        var partIndex = 1

        while (offset < text.length) {
            val end = (offset + maxChunkSize).coerceAtMost(text.length)
            val partText = text.substring(offset, end)
            val partStartLine = baseStartLine + text.substring(0, offset).count { it == '\n' }
            result.add(
                AstChunk(
                    text = partText,
                    startLine = partStartLine,
                    declarationName = declarationName?.let { "$it#part$partIndex" } ?: "#part$partIndex",
                    parentScope = parentScope,
                    isFun = isFun,
                    isSplit = true,
                )
            )
            partIndex++
            if (end == text.length) break
            offset += maxChunkSize - overlap
        }

        return result
    }

    /**
     * Добавляет чанк(и) для простой декларации — top-level свойства, type_alias и подобных.
     *
     * В большинстве случаев создаёт один чанк: ведущий комментарий + текст декларации.
     * Исключение: если комментарий настолько длинный, что вместе с декларацией они
     * не помещаются в [maxChunkSize], они разбиваются на два отдельных чанка с одинаковым
     * [declarationName] (чтобы [mergeGreedy] мог объединить их обратно, когда это возможно).
     *
     * Не используется для функций (→ [collectFunctionChunks]) и контейнеров (→ [buildContainerChunk]).
     */
    private fun addDeclarationChunks(
        sink: MutableList<AstChunk>,
        declarationText: String,
        declarationName: String?,
        parentScope: String?,
        declarationStartLine: Int,
        leadingComments: String,
        commentsStartLine: Int,
        nodeType: String? = null,
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
                    nodeType = nodeType,
                )
            )
            sink.add(
                AstChunk(
                    text = cleanDeclaration,
                    startLine = declarationStartLine,
                    declarationName = declarationName,
                    parentScope = parentScope,
                    nodeType = nodeType,
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
                nodeType = nodeType,
            )
        )
    }

    /**
     * Возвращает текст заголовка декларации — без тела класса.
     *
     * Для контейнеров ([AST_CONTAINER_TYPES]) вырезает байты от начала узла до начала тела
     * (`class_body` / `object_body`). Тело в tree-sitter начинается с `{`, поэтому результат
     * содержит сигнатуру класса без открывающей скобки.
     * Пример: `public abstract class AIAgent<Input, Output> : Closeable`
     *
     * Для не-контейнеров (функции, свойства) возвращает полный текст узла через [nodeText].
     *
     * Используется в [buildContainerChunk] для построения классового чанка. Открывающий `{`
     * добавляется там вручную при необходимости (когда в чанк включаются свойства).
     */
    private fun declarationHeaderText(node: Node, sourceBytes: ByteArray): String {
        if (node.type !in AST_CONTAINER_TYPES) return KotlinAstParser.nodeText(node, sourceBytes).trimEnd()

        val body = node.namedChildren.firstOrNull { it.type in AST_BODY_TYPES }
        if (body == null) return KotlinAstParser.nodeText(node, sourceBytes).trimEnd()

        val start = node.startByte.toInt()
        val end = body.startByte.toInt().coerceAtMost(sourceBytes.size)
        if (start >= end) return KotlinAstParser.nodeText(node, sourceBytes).trimEnd()
        return String(sourceBytes, start, end - start, Charsets.UTF_8).trimEnd()
    }

    /**
     * Извлекает имя декларации из AST-узла.
     *
     * Стратегия поиска (от наиболее конкретного к общему):
     * 1. Поле `name` в грамматике tree-sitter (работает для большинства деклараций: class, fun, typealias).
     * 2. Для `property_declaration` — спускается в `variable_declaration` → поле `name`
     *    или дочерний `simple_identifier` (нужно для деструктурирующих объявлений).
     * 3. `type_identifier` — имя типа (резервный вариант для сложных узлов).
     * 4. `simple_identifier` / `identifier` / `object_identifier` — самый общий вариант.
     *
     * Возвращает null, если имя не удалось извлечь ни одним способом.
     */
    private fun extractDeclarationName(node: Node, sourceBytes: ByteArray): String? {
        node.childByFieldName("name")?.let {
            return KotlinAstParser.nodeText(it, sourceBytes).ifBlank { null }
        }

        if (node.type == "property_declaration") {
            node.namedChildren.firstOrNull { it.type == "variable_declaration" }?.let { varDecl ->
                val nameNode = varDecl.childByFieldName("name")
                    ?: varDecl.namedChildren.firstOrNull { it.type == "simple_identifier" }
                nameNode?.let { return KotlinAstParser.nodeText(it, sourceBytes).ifBlank { null } }
            }
        }

        node.namedChildren.firstOrNull { it.type == "type_identifier" }?.let {
            return KotlinAstParser.nodeText(it, sourceBytes).ifBlank { null }
        }

        node.namedChildren.firstOrNull {
            it.type in setOf("simple_identifier", "identifier", "object_identifier")
        }?.let {
            return KotlinAstParser.nodeText(it, sourceBytes).ifBlank { null }
        }

        return null
    }

    /**
     * Merges adjacent chunks that share the same [AstChunk.parentScope] and fit within [maxChunkSize].
     * The merged chunk keeps the first chunk's [AstChunk.declarationName].
     * Chunks at different scopes are never merged.
     */
    private fun mergeGreedy(candidates: List<AstChunk>): List<AstChunk> {
        if (candidates.isEmpty()) return emptyList()

        val result = mutableListOf<AstChunk>()
        var acc = candidates[0]

        for (i in 1 until candidates.size) {
            val next = candidates[i]
            val accBase = acc.declarationName?.substringBefore("#")
            val nextBase = next.declarationName?.substringBefore("#")
            // Split parts must only merge with other parts of the same declaration, never with siblings.
            val splitBarrier = (acc.isSplit || next.isSplit) && accBase != nextBase
            if (!splitBarrier &&
                acc.parentScope == next.parentScope &&
                (acc.text.length + next.text.length + 2) <= maxChunkSize
            ) {
                val sameName = acc.declarationName == next.declarationName
                acc = acc.copy(
                    text = acc.text + "\n\n" + next.text,
                    declarationName = if (sameName) acc.declarationName else null,
                    isFun = sameName && acc.isFun,
                    isSplit = sameName && acc.isSplit,
                )
            } else {
                result.add(acc)
                acc = next
            }
        }

        result.add(acc)
        return result
    }
}
