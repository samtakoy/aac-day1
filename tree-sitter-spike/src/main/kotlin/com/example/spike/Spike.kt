package com.example.spike

import com.example.spike.grammar.KotlinLanguage
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Node
import io.github.treesitter.ktreesitter.Parser

fun main() {
    println("=== ktreesitter spike: Kotlin grammar ===")

    // Шаг 1: JNI грамматики загружается?
    println("\n[1] Loading Kotlin grammar native library...")
    val language = try {
        Language(KotlinLanguage.language())
    } catch (e: UnsatisfiedLinkError) {
        println("    FAIL UnsatisfiedLinkError: ${e.message}")
        return
    } catch (e: Exception) {
        println("    FAIL ${e::class.simpleName}: ${e.message}")
        return
    }
    println("    OK: Language loaded, symbolCount=${language.symbolCount}, fieldCount=${language.fieldCount}")

    // Шаг 2: Парсим Kotlin-код
    println("\n[2] Parsing Kotlin source...")
    val parser = Parser(language)
    val source = """
        package com.example

        class GraphAIAgent(val name: String) {
            fun runSession(input: String): String {
                return "result"
            }

            companion object {
                fun create() = GraphAIAgent("default")
            }
        }

        fun topLevelFun() = 42
    """.trimIndent()

    val tree = parser.parse(source)
    val root = tree.rootNode
    println("    Root node type: ${root.type}")
    println("    Has errors: ${root.hasError}")
    println("    Child count: ${root.childCount}")

    // Шаг 3: Top-level объявления
    println("\n[3] Top-level declarations (companion object должен отсутствовать):")
    root.children.filter { it.isNamed }.forEach { child ->
        val name = child.childByFieldName("name")?.text() ?: "<no name>"
        println("    [top] type=${child.type} name=$name " +
                "lines=${child.startPoint.row.toInt() + 1}..${child.endPoint.row.toInt() + 1}")
    }

    // Шаг 4: Внутри класса — companion должен быть здесь
    println("\n[4] Inside class body (companion object MUST be here):")
    val classNode = root.children.firstOrNull { it.type == "class_declaration" }
    val body = classNode?.childByFieldName("body")
    body?.children?.filter { it.isNamed }?.forEach { child ->
        val name = child.childByFieldName("name")?.text() ?: "<no name>"
        println("    [in class] type=${child.type} name=$name")
    }

    // Шаг 5: Проверяем что можем взять source text ноды
    println("\n[5] Source text extraction:")
    classNode?.let {
        val src = it.text()
        println("    class source length: ${src?.length}")
        println("    first 60 chars: ${src?.toString()?.take(60)?.replace('\n', ' ')}")
    }

    println("\n=== spike SUCCESS ===")
}
