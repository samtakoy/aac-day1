package com.example.day.ragserver.indexing.chunking

import com.example.day.ragserver.indexing.chunking.DEFAULT_MAX_CHUNK_SIZE
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class MarkdownChunkingStrategyTest {
    private val codePath = "/Users/samtakot/devs/learnings/aiadvent/day1/androidprj/aac-day1_kimi/app/src/main/java/com/example/day"
    
    private val fileStrategy = MarkdownChunkingStrategy(
        maxChunkSize = 2000,
        codePath = codePath
    )

    @Test
    fun test1() {
        testChunkFile("/Users/samtakot/devs/learnings/aiadvent/day1/androidprj/aac-day1_kimi/app/src/main/java/com/example/day/core/core_features/memory/Module.md")
    }

    fun testChunkFile(filePath: String) {
        val file = File(filePath)
        assertTrue(file.exists(), "File not found: $filePath")
        val (content, label) = file.readText() to file.name

        val chunks = fileStrategy.split(content, label, label)

        println("\n=== AST chunks for: $label (${chunks.size} chunks) ===")
        chunks.forEachIndexed { i, chunk ->
            println("━━━ #${i + 1}  decl=${chunk.declarationName ?: "<none>"}  parent=${chunk.parentScope ?: "null"}  line=${chunk.startLine}  len=${chunk.content.length}")
            println(chunk.content)
            println()
        }

        assertTrue(chunks.isNotEmpty(), "Expected at least one chunk")
    }
}