package com.example.day.ragserver.indexing

import com.example.day.ragserver.db.ClassMetadata
import kotlinx.serialization.json.Json

class MetadataExtractor(
    private val llmProvider: LlmProvider,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun extract(fileContent: String, className: String, packageName: String = ""): ClassMetadata? {
        val prompt = buildPrompt(fileContent.take(6000), className, packageName)
        return try {
            val response = llmProvider.generate(prompt)
            val cleaned = cleanJson(response)
            val normalized = normalizeStringFields(cleaned)
            val metadata = json.decodeFromString<ClassMetadata>(normalized)
            MetadataValidator.validate(metadata)
        } catch (e: Exception) {
            println("[MetadataExtractor] Failed for $className: ${e.message}")
            null
        }
    }

    private fun buildPrompt(code: String, className: String, packageName: String): String = """
        Analyze this Kotlin file.
        File context:
          Class: $className
          Package: ${packageName.ifEmpty { "unknown" }}

        Return ONLY a JSON object. No text outside JSON. No markdown.

        Schema:
        {
          "class_name": "$className",
          "responsibility": string,
          "dependencies": string[],
          "key_methods": [{"name": string, "description": string, "params": string|null}],
          "domain_tags": string[]
        }

        Rules:
        - class_name: MUST be exactly "$className"
        - responsibility: MUST be a string (NEVER []). ONE sentence, max 15 words.
          If file contains nested/helper classes, describe the primary class only.
          If class name is unclear — infer responsibility from the name.
        - dependencies: real class names from constructors, properties, function params
        - key_methods: ONLY functions/methods (fun keyword) actually present in the primary class (NOT properties, NOT val/var fields.), max 5
          - name: method name only, no parentheses
          - description: one sentence, max 10 words
          - params: parameter list as "name: Type, name: Type" or null if no params
        - domain_tags: 1-3 categories, e.g. "Chat", "Auth", "User Management"
        - For empty fields use [] (only for arrays, never for strings)
        - Do NOT invent names

        Example key_methods entry:
        {"name": "handleRuntest", "description": "runs test questions through RAG pipeline", "params": "agentId: Long, presetArg: String?, chat: Chat"}

        Code:
        $code
    """.trimIndent()

    // LLM иногда возвращает [] для responsibility когда не знает что написать.
    // Нормализуем до пустой строки чтобы не падал JSON-парсер.
    private fun normalizeStringFields(json: String): String = json
        .replace(Regex(""""responsibility"\s*:\s*\[]"""), """"responsibility": """")

    private fun cleanJson(response: String): String {
        val text = response.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start != -1 && end != -1 && end > start) text.substring(start, end + 1) else text
    }
}
