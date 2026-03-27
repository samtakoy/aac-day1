package com.example.day.ragserver.indexing

import com.example.day.ragserver.indexing.chunking.ast.AstChunkingStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Additional AST chunking tests for various Kotlin declaration types.
 * These tests verify that the chunking strategy correctly extracts
 * different types of declarations from Kotlin source code.
 */
class AdditionalAstChunkingTest {

    // maxChunkSize=30: small enough so even short test snippets exceed it and trigger AST splitting,
    // but large enough for individual one-liner declarations to not be sub-split internally.
    private val strategy = AstChunkingStrategy.create(maxChunkSize = 30)

    // -------------------------------------------------------------------
    // Test: Interface declarations
    // -------------------------------------------------------------------
    @Test
    fun testInterfaceDeclarations() {
        val code = """
            package test

            interface ClickListener {
                fun onClick()
                fun onLongClick()
            }
        """.trimIndent()

        val chunks = strategy.split(code, "test.kt", "test.kt")

        println("\n=== testInterfaceDeclarations (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}")
        }

        assertTrue(chunks.any { it.declarationName == "ClickListener" }, "Expected interface chunk")
    }

    // -------------------------------------------------------------------
    // Test: Object declarations
    // -------------------------------------------------------------------
    @Test
    fun testObjectDeclarations() {
        val code = """
            package test

            object Config {
                val version = "1.0"
                fun load() = Unit
            }
        """.trimIndent()

        val chunks = strategy.split(code, "test.kt", "test.kt")

        println("\n=== testObjectDeclarations (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}")
        }

        assertTrue(chunks.any { it.declarationName == "Config" }, "Expected object chunk")
        assertTrue(chunks.any { it.declarationName == "version" && it.parentScope == "Config" }, "Expected property chunk")
    }

    // -------------------------------------------------------------------
    // Test: Data classes
    // -------------------------------------------------------------------
    @Test
    fun testDataClasses() {
        val code = """
            package test

            data class User(val name: String, val age: Int)
        """.trimIndent()

        val chunks = strategy.split(code, "test.kt", "test.kt")

        println("\n=== testDataClasses (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}")
        }

        assertTrue(chunks.any { it.declarationName == "User" }, "Expected data class chunk")
    }

    // -------------------------------------------------------------------
    // Test: Enum classes
    // -------------------------------------------------------------------
    @Test
    fun testEnumClasses() {
        val code = """
            package test

            enum class Color {
                RED, GREEN, BLUE
            }
        """.trimIndent()

        val chunks = strategy.split(code, "test.kt", "test.kt")

        println("\n=== testEnumClasses (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}")
        }

        assertTrue(chunks.any { it.declarationName == "Color" }, "Expected enum chunk")
    }

    // -------------------------------------------------------------------
    // Test: Sealed classes
    // -------------------------------------------------------------------
    @Test
    fun testSealedClasses() {
        val code = """
            package test

            sealed class Result
            class Success(val data: String) : Result()
            class Error(val msg: String) : Result()
        """.trimIndent()

        val chunks = strategy.split(code, "test.kt", "test.kt")

        println("\n=== testSealedClasses (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}")
        }

        assertTrue(chunks.any { it.declarationName == "Result" }, "Expected sealed class chunk")
    }

    // -------------------------------------------------------------------
    // Test: Abstract classes
    // -------------------------------------------------------------------
    @Test
    fun testAbstractClasses() {
        val code = """
            package test

            abstract class Base {
                abstract fun doSomething()
            }
        """.trimIndent()

        val chunks = strategy.split(code, "test.kt", "test.kt")

        println("\n=== testAbstractClasses (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}")
        }

        assertTrue(chunks.any { it.declarationName == "Base" }, "Expected abstract class chunk")
    }

    // -------------------------------------------------------------------
    // Test: Nested/Inner classes
    // -------------------------------------------------------------------
    @Test
    fun testNestedClasses() {
        val code = """
            package test

            class Outer {
                class Inner
                
                inner class InnerWithOuter {
                    fun accessOuter() = Unit
                }
            }
        """.trimIndent()

        val chunks = strategy.split(code, "test.kt", "test.kt")

        println("\n=== testNestedClasses (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}")
        }

        assertTrue(chunks.any { it.declarationName == "Outer" && it.parentScope == null }, "Expected Outer class")
        assertTrue(chunks.any { it.declarationName == "Inner" && it.parentScope == "Outer" }, "Expected Inner class")
        assertTrue(chunks.any { it.declarationName == "InnerWithOuter" && it.parentScope == "Outer" }, "Expected InnerWithOuter class")
    }

    // -------------------------------------------------------------------
    // Test: Generic classes
    // -------------------------------------------------------------------
    @Test
    fun testGenericClasses() {
        val code = """
            package test

            class Box<T>(val value: T)
            class Pair<K, V>(val key: K, val value: V)
        """.trimIndent()

        val chunks = strategy.split(code, "test.kt", "test.kt")

        println("\n=== testGenericClasses (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}")
        }

        assertTrue(chunks.any { it.declarationName == "Box" }, "Expected Box class")
        assertTrue(chunks.any { it.declarationName == "Pair" }, "Expected Pair class")
    }

    // -------------------------------------------------------------------
    // Test: Functions with various modifiers
    // -------------------------------------------------------------------
    @Test
    fun testFunctionModifiers() {
        val code = """
            package test

            suspend fun asyncTask() {}
            inline fun inlined() {}
            infix fun String.concat(other: String) = this + other
            external fun nativeCall()
        """.trimIndent()

        val chunks = strategy.split(code, "test.kt", "test.kt")

        println("\n=== testFunctionModifiers (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}")
        }

        assertTrue(chunks.any { it.declarationName == "asyncTask" }, "Expected suspend function")
        assertTrue(chunks.any { it.declarationName == "inlined" }, "Expected inline function")
        assertTrue(chunks.any { it.declarationName == "concat" }, "Expected infix function")
    }

    // -------------------------------------------------------------------
    // Test: Extension functions
    // -------------------------------------------------------------------
    @Test
    fun testExtensionFunctions() {
        val code = """
            package test

            fun String.reversed(): String = this.reversed()
            fun Int.square(): Int = this * this
        """.trimIndent()

        val chunks = strategy.split(code, "test.kt", "test.kt")

        println("\n=== testExtensionFunctions (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}")
        }

        assertTrue(chunks.any { it.declarationName == "reversed" }, "Expected reversed extension function")
        assertTrue(chunks.any { it.declarationName == "square" }, "Expected square extension function")
    }

    // -------------------------------------------------------------------
    // Test: Properties (val/var)
    // -------------------------------------------------------------------
    @Test
    fun testProperties() {
        val code = """
            package test

            class Container {
                val immutable: String = "value"
                var mutable: Int = 0
                lateinit var lateInit: String
                val lazy: String by lazy { "computed" }
            }
        """.trimIndent()

        val chunks = strategy.split(code, "test.kt", "test.kt")

        println("\n=== testProperties (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}")
        }

        assertTrue(chunks.any { it.declarationName == "Container" }, "Expected Container class")
        assertTrue(chunks.any { it.declarationName == "immutable" && it.parentScope == "Container" }, "Expected val property")
        assertTrue(chunks.any { it.declarationName == "mutable" && it.parentScope == "Container" }, "Expected var property")
    }

    // -------------------------------------------------------------------
    // Test: Companion objects
    // -------------------------------------------------------------------
    @Test
    fun testCompanionObjects() {
        val code = """
            package test

            class MyClass {
                companion object {
                    const val TAG = "MyClass"
                    fun factory() = MyClass()
                }
            }
        """.trimIndent()

        val chunks = strategy.split(code, "test.kt", "test.kt")

        println("\n=== testCompanionObjects (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}")
        }

        assertTrue(chunks.any { it.declarationName == "MyClass" }, "Expected MyClass")
        // Companion object may have null name or special handling
    }

    // -------------------------------------------------------------------
    // Test: Annotations on declarations
    // -------------------------------------------------------------------
    @Test
    fun testAnnotations() {
        val code = """
            package test

            @Deprecated("Use newMethod")
            @Suppress("UNUSED")
            fun oldMethod() {}

            @Inject
            class MyService
        """.trimIndent()

        val chunks = strategy.split(code, "test.kt", "test.kt")

        println("\n=== testAnnotations (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}")
            println("  content preview: ${chunk.content.take(100)}")
        }

        val methodChunk = chunks.find { it.declarationName == "oldMethod" }
        assertNotNull(methodChunk, "Expected oldMethod chunk")
        assertTrue(methodChunk.content.contains("@Deprecated"), "Expected @Deprecated annotation")

        val classChunk = chunks.find { it.declarationName == "MyService" }
        assertNotNull(classChunk, "Expected MyService chunk")
        assertTrue(classChunk.content.contains("@Inject"), "Expected @Inject annotation")
    }

    // -------------------------------------------------------------------
    // Test: KDoc comments
    // -------------------------------------------------------------------
    @Test
    fun testKDocComments() {
        val code = """
            package test

            /**
             * This is a KDoc comment for the class.
             * It has multiple lines.
             */
            class DocumentedClass
            
            /** Single line KDoc */
            fun documentedFunction() {}
        """.trimIndent()

        val chunks = strategy.split(code, "test.kt", "test.kt")

        println("\n=== testKDocComments (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}")
            println("  content preview: ${chunk.content.take(150)}")
        }

        val classChunk = chunks.find { it.declarationName == "DocumentedClass" }
        assertNotNull(classChunk, "Expected DocumentedClass chunk")
        assertTrue(classChunk.content.contains("KDoc comment"), "Expected KDoc in class chunk")

        val funcChunk = chunks.find { it.declarationName == "documentedFunction" }
        assertNotNull(funcChunk, "Expected documentedFunction chunk")
        assertTrue(funcChunk.content.contains("Single line KDoc"), "Expected KDoc in function chunk")
    }

    // -------------------------------------------------------------------
    // Test: Type aliases
    // -------------------------------------------------------------------
    @Test
    fun testTypeAliases() {
        val code = """
            package test

            typealias Handler = () -> Unit
            typealias StringMap = Map<String, String>
        """.trimIndent()

        val chunks = strategy.split(code, "test.kt", "test.kt")

        println("\n=== testTypeAliases (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}")
        }

        assertTrue(chunks.any { it.declarationName == "Handler" }, "Expected Handler typealias")
        assertTrue(chunks.any { it.declarationName == "StringMap" }, "Expected StringMap typealias")
    }

    // -------------------------------------------------------------------
    // Test: Mixed declarations
    // -------------------------------------------------------------------
    @Test
    fun testMixedDeclarations() {
        val code = """
            package com.example

            /** Application config */
            object AppConfig {
                const val DEBUG = true
            }

            interface Repository<T> {
                fun get(id: String): T
                fun save(item: T)
            }

            data class User(val name: String, val email: String)

            class UserRepository : Repository<User> {
                override fun get(id: String) = User("name", "email")
                override fun save(item: User) {}
            }

            fun topLevelFunc() = 42
        """.trimIndent()

        val chunks = strategy.split(code, "test.kt", "test.kt")

        println("\n=== testMixedDeclarations (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}")
        }

        // Verify all expected declarations are present
        assertTrue(chunks.any { it.declarationName == "AppConfig" }, "Expected AppConfig object")
        assertTrue(chunks.any { it.declarationName == "Repository" }, "Expected Repository interface")
        assertTrue(chunks.any { it.declarationName == "User" }, "Expected User data class")
        assertTrue(chunks.any { it.declarationName == "UserRepository" }, "Expected UserRepository class")
        assertTrue(chunks.any { it.declarationName == "topLevelFunc" && it.parentScope == null }, "Expected top-level function")
    }

    // -------------------------------------------------------------------
    // Test: Empty and minimal files (edge cases)
    // -------------------------------------------------------------------
    @Test
    fun testEdgeCases() {
        // Empty file should return empty list
        val emptyChunks = strategy.split("", "empty.kt", "empty.kt")
        assertTrue(emptyChunks.isEmpty(), "Expected empty list for empty file")

        // File with only whitespace
        val whitespaceChunks = strategy.split("   \n\n   ", "ws.kt", "ws.kt")
        assertTrue(whitespaceChunks.isEmpty(), "Expected empty list for whitespace-only file")

        // Single line class — too short to trigger AST splitting (content.length < maxChunkSize),
        // returned as-is with no declarationName. Just ensure it's not empty.
        val singleLineChunks = strategy.split("class A", "single.kt", "single.kt")
        assertTrue(singleLineChunks.isNotEmpty(), "Expected at least one chunk for single-class file")
    }

    // -------------------------------------------------------------------
    // Test: Large file (multiple chunks)
    // -------------------------------------------------------------------
    @Test
    fun testLargeFile() {
        val code = buildString {
            appendLine("package test")
            appendLine()
            repeat(10) { i ->
                appendLine("class Class$i { fun method$i() = $i }")
            }
        }

        val chunks = strategy.split(code, "large.kt", "large.kt")

        println("\n=== testLargeFile (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  len=${chunk.content.length}")
        }

        // Should have multiple chunks since each class is small
        assertTrue(chunks.size > 1, "Expected multiple chunks for large file")
    }

    // -------------------------------------------------------------------
    // Test: Properties holding lambdas are extracted as property_declaration chunks.
    // Anonymous lambda literals themselves (lambda_literal) are NOT declaration nodes,
    // so they never become standalone chunks — only the enclosing property does.
    // -------------------------------------------------------------------
    @Test
    fun testPropertyDeclarationsWithLambdas() {
        val code = """
            package test

            class Handler {
                val callback = { x: Int -> x + 1 }
                val lambda: () -> Unit = { println("hi") }
            }
        """.trimIndent()

        val chunks = strategy.split(code, "test.kt", "test.kt")

        println("\n=== testPropertyDeclarationsWithLambdas (${chunks.size} chunks) ===")
        chunks.forEach { chunk ->
            println("decl=${chunk.declarationName}  parent=${chunk.parentScope}")
        }

        // Handler class is extracted
        assertTrue(chunks.any { it.declarationName == "Handler" }, "Expected Handler class chunk")
        // At least one property from inside Handler is present (callback and lambda may be merged)
        assertTrue(
            chunks.any { it.parentScope == "Handler" && it.content.contains("callback") },
            "Expected callback property content inside a Handler chunk"
        )
    }
}
