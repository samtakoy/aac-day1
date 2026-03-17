package com.example.day.ragserver.indexing

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class OllamaLlmProvider(
    private val baseUrl: String,
    private val model: String,
    private val httpClient: HttpClient,
) : LlmProvider {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generate(prompt: String): String {
        val response = httpClient.post("$baseUrl/api/generate") {
            contentType(ContentType.Application.Json)
            setBody(OllamaGenerateRequest(model = model, prompt = prompt, stream = false))
        }
        if (!response.status.value.toString().startsWith("2")) {
            error("Ollama generate failed: HTTP ${response.status.value}")
        }
        // Ollama returns application/x-ndjson — multiple JSON objects, one per line.
        // Each line: {"response":"chunk","done":false}, last line has "done":true.
        val raw = response.body<String>()
        val result = raw.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                runCatching { json.decodeFromString<OllamaGenerateResponse>(line) }.getOrNull()
            }
            .joinToString("") { it.response }
        // TODO remove
        println("[OllamaLlm] <<<\n$result\n>>>")
        return result
    }
}

@Serializable
private data class OllamaGenerateRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
)

@Serializable
private data class OllamaGenerateResponse(
    val response: String,
)
