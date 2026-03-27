package com.example.day.ragserver.indexing

import com.example.day.ragserver.indexing.chunking.ast.AstChunkingStrategy
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AstChunkingTest {

    // Small maxChunkSize so AST splitting kicks in even for short snippets
    private val strategy = AstChunkingStrategy.create(maxChunkSize = 100)

    // -------------------------------------------------------------------
    // Test 1: Diagnostic — point at any file via -Dchunk.file=<path>
    // Falls back to built-in snippet if property is not set.
    // Always passes; output is visible via --info or HTML report.
    // -------------------------------------------------------------------
    @Test
    fun testChunkFile() {
        val filePath = System.getProperty("chunk.file")
        val (content, label) = if (filePath != null) {
            val file = File(filePath)
            assertTrue(file.exists(), "File not found: $filePath")
            file.readText() to file.name
        } else {
            BUILTIN_SNIPPET to "builtin_snippet.kt"
        }

        val chunks = strategy.split(content, label, label)

        println("\n=== AST chunks for: $label (${chunks.size} chunks) ===")
        chunks.forEachIndexed { i, chunk ->
            val preview = chunk.content.lines().take(2).joinToString(" ↵ ").take(120)
            println(
                "#${i + 1}  " +
                "decl=${chunk.declarationName?.padEnd(30) ?: "<none>".padEnd(30)}  " +
                "parent=${(chunk.parentScope ?: "null").padEnd(25)}  " +
                "line=${chunk.startLine.toString().padEnd(4)}  " +
                "len=${chunk.content.length}"
            )
            println("    $preview")
            println()
        }

        assertTrue(chunks.isNotEmpty(), "Expected at least one chunk")
    }

    // -------------------------------------------------------------------
    // Test 2: Deterministic assertions on the built-in snippet
    // -------------------------------------------------------------------
    @Test
    fun testBuiltinSnippetChunks() {
        val chunks = strategy.split(BUILTIN_SNIPPET, "builtin_snippet.kt", "builtin_snippet.kt")

        println("\n=== testBuiltinSnippetChunks (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}  line=${chunk.startLine}")
            println(chunk.content.take(1200))
            println("---")
        }

        assertTrue(
            chunks.any { it.declarationName == "GraphAIAgent" },
            "Expected chunk for class GraphAIAgent"
        )
        assertTrue(
            chunks.any { it.declarationName == "runSession" && it.parentScope == "GraphAIAgent" },
            "Expected runSession with parentScope=GraphAIAgent"
        )
        // topLevelFun may be merged with create() (both parentScope=null, fit within maxChunkSize).
        // Assert on content rather than declarationName, which belongs to the first in a merged group.
        assertTrue(
            chunks.any { it.content.contains("fun topLevelFun()") && it.parentScope == null },
            "Expected topLevelFun content with no parentScope"
        )

        val classChunk = chunks.first { it.declarationName == "GraphAIAgent" }
        assertTrue(
            classChunk.content.contains("/** Главный агент */"),
            "Expected KDoc to be included in class chunk.\nActual content:\n${classChunk.content}"
        )

        val methodChunk = chunks.first { it.declarationName == "runSession" }
        assertTrue(
            methodChunk.content.contains("// запускает сессию"),
            "Expected single-line comment to be included in method chunk.\nActual content:\n${methodChunk.content}"
        )
    }

    companion object {
        private val BUILTIN_SNIPPET = """
            package com.example

            /** Главный агент */
            class GraphAIAgent(val name: String) {
                // запускает сессию
                fun runSession(input: String): String {
                    return "result"
                }

                companion object {
                    fun create() = GraphAIAgent("default")
                }
            }

            fun topLevelFun() = 42
        """.trimIndent()
    }
}
