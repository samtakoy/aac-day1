package com.example.day.ragserver.indexing.chunking.ast

// Источник истины: fwcd/tree-sitter-kotlin grammar (https://github.com/fwcd/tree-sitter-kotlin)
// Важно: interface/enum/abstract/sealed — это class_declaration с модификатором, не отдельные типы.
// type_alias (НЕ typealias_declaration) — правильное имя из грамматики.

/** Типы нод, которые становятся отдельными чанками. */
internal val AST_INTERESTING_TYPES = setOf(
    "class_declaration",    // class, interface, enum, sealed, abstract, data
    "object_declaration",   // object MySingleton
    "companion_object",     // companion object
    "function_declaration", // fun
    "property_declaration", // val, var
    "type_alias",           // typealias (НЕ typealias_declaration!)
)

/** Типы нод, внутрь которых нужно рекурсировать для извлечения вложенных деклараций. */
internal val AST_CONTAINER_TYPES = setOf(
    "class_declaration",
    "object_declaration",
    "companion_object",
)

/** Типы нод, которые являются «телом» контейнера (содержат дочерние декларации). */
internal val AST_BODY_TYPES = setOf(
    "class_body",
    "object_body",
)

/** Типы нод, считающихся leading-комментариями (KDoc, //, /* */). */
internal val AST_COMMENT_TYPES = setOf(
    "multiline_comment",
    "line_comment",
)

/** Named nodes that may appear between a comment block and its declaration. */
internal val AST_COMMENT_BRIDGE_TYPES = setOf(
    "annotation",
    "modifiers",
)
