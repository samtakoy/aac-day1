package com.example.day.ragserver.indexing.chunking.ast

import com.example.day.raggrammar.KotlinLanguage
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Node
import io.github.treesitter.ktreesitter.Parser

/**
 * Общий синглтон для парсинга Kotlin-кода через tree-sitter.
 *
 * Парсер не является потокобезопасным — все вызовы [parse] обёрнуты в [synchronized].
 * Создаётся лениво при первом обращении.
 */
object KotlinAstParser {

    private val parser: Parser by lazy {
        val language = Language(KotlinLanguage.language())
        Parser(language)
    }

    /**
     * Парсит исходный код Kotlin-файла и возвращает корневой узел AST-дерева.
     * Возвращает пару: (корневой узел, байтовое представление источника для [nodeText]).
     */
    fun parse(source: String): Pair<Node, ByteArray> {
        val sourceBytes = source.toByteArray(Charsets.UTF_8)
        val tree = synchronized(parser) { parser.parse(source) }
        return tree.rootNode to sourceBytes
    }

    /**
     * Извлекает исходный текст AST-узла из байтового массива источника.
     *
     * tree-sitter хранит позиции в байтах, а не символах. Для корректной работы с
     * non-ASCII исходниками необходимо работать с байтовым массивом.
     */
    fun nodeText(node: Node, sourceBytes: ByteArray): String {
        val start = node.startByte.toInt()
        val end = node.endByte.toInt().coerceAtMost(sourceBytes.size)
        if (start >= end) return ""
        return String(sourceBytes, start, end - start, Charsets.UTF_8)
    }
}
