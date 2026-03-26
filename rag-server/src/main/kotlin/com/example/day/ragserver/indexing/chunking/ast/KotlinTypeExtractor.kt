package com.example.day.ragserver.indexing.chunking.ast

import io.github.treesitter.ktreesitter.Node

/**
 * Извлекает типы классов, используемых в primary constructor и val/var свойствах
 * top-level деклараций Kotlin-файла.
 *
 * Используется в Phase B [IndexingService] для построения графа зависимостей (usedBy).
 * Возвращает сырые имена типов без фильтрации — отбор проектных классов
 * (пересечение с knownClasses) выполняется в IndexingService.
 *
 * AST-узлы Kotlin grammar (fwcd/tree-sitter-kotlin):
 * - primary_constructor → value_parameter → type_reference → type_identifier / user_type
 * - class_body → property_declaration → type_reference → type_identifier / user_type
 */
object KotlinTypeExtractor {

    fun extractReferencedTypes(fileContent: String): List<String> {
        val (root, sourceBytes) = KotlinAstParser.parse(fileContent)
        return buildList {
            collectTopLevelContainers(root).forEach { containerNode ->
                addAll(extractConstructorParamTypes(containerNode, sourceBytes))
                val body = containerNode.namedChildren.firstOrNull { it.type in setOf("class_body", "object_body") }
                if (body != null) {
                    addAll(extractPropertyTypes(body, sourceBytes))
                }
            }
        }.distinct()
    }

    /**
     * Собирает top-level class_declaration и object_declaration из корня файла.
     * Вложенные классы не включаются — нас интересуют только прямые зависимости файла.
     */
    private fun collectTopLevelContainers(root: Node): List<Node> {
        return root.namedChildren.filter { node ->
            node.isNamed && node.type in setOf("class_declaration", "object_declaration")
        }
    }

    /**
     * Извлекает типы из primary_constructor → value_parameter → type_reference.
     *
     * Пример: `class Foo(private val bar: BarService, baz: BazRepository)`
     * → ["BarService", "BazRepository"]
     */
    private fun extractConstructorParamTypes(containerNode: Node, sourceBytes: ByteArray): List<String> {
        val constructor = containerNode.namedChildren.firstOrNull { it.type == "primary_constructor" }
            ?: return emptyList()

        return constructor.namedChildren
            .filter { it.type == "value_parameter" }
            .flatMap { param -> extractTypeNames(param, sourceBytes) }
    }

    /**
     * Извлекает типы из property_declaration в теле класса/объекта.
     *
     * Пример: `val repo: ChatRepository` → ["ChatRepository"]
     */
    private fun extractPropertyTypes(body: Node, sourceBytes: ByteArray): List<String> {
        return body.namedChildren
            .filter { it.type == "property_declaration" }
            .flatMap { prop -> extractTypeNames(prop, sourceBytes) }
    }

    /**
     * Рекурсивно ищет type_reference → user_type → type_identifier внутри узла.
     * Возвращает простые имена типов (без пакетного пути и generics).
     *
     * Примеры:
     * - `BarService` → "BarService"
     * - `List<ChunkEntity>` → "List", "ChunkEntity"  (берём все type_identifier)
     * - `Map<String, Any>` → "Map", "String", "Any"
     */
    private fun extractTypeNames(node: Node, sourceBytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        collectTypeIdentifiers(node, sourceBytes, result)
        return result
    }

    private fun collectTypeIdentifiers(node: Node, sourceBytes: ByteArray, result: MutableList<String>) {
        if (node.type == "type_identifier") {
            val name = KotlinAstParser.nodeText(node, sourceBytes).trim()
            if (name.isNotBlank() && name[0].isUpperCase()) {
                result.add(name)
            }
            return
        }
        for (child in node.namedChildren) {
            collectTypeIdentifiers(child, sourceBytes, result)
        }
    }
}
