package com.example.day.ragserver.indexing

/**
 * Pure utility functions for extracting structural metadata from Kotlin source files.
 * All functions are stateless and side-effect-free.
 */
object CodeMetadata {

    private val PACKAGE_REGEX = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)

    // Captures the identifier right after a primary declaration keyword.
    // Intentionally simple: no need to handle every edge case for metadata enrichment.
    private val DECLARATION_REGEX = Regex(
        """(?:fun|class|interface|object|typealias)\s+(\w+)"""
    )

    /** Returns the `package` name from file content, or empty string if absent. */
    fun extractPackage(fileContent: String): String =
        PACKAGE_REGEX.find(fileContent)?.groupValues?.get(1) ?: ""

    /**
     * Returns the first top-level declaration name in a code block
     * (`fun`, `class`, `interface`, `object`, `typealias`).
     * Returns null for blocks with no declaration (e.g. imports-only header block).
     */
    fun extractDeclarationName(blockText: String): String? =
        DECLARATION_REGEX.find(blockText)?.groupValues?.get(1)
}
