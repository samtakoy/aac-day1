package com.example.day.aigateway.llm

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap

private const val DEFAULT_NUM_CTX = 4096

class OllamaModelInfoCache(
    private val client: HttpClient,
    private val ollamaUrl: String
) {
    private val cache = ConcurrentHashMap<String, Int>()
    private val mutex = Mutex()

    suspend fun numCtxFor(model: String): Int {
        cache[model]?.let { return it }
        return mutex.withLock {
            cache[model] ?: try {
                fetchNumCtx(model).also { cache[model] = it }
            } catch (e: Exception) {
                DEFAULT_NUM_CTX
            }
        }
    }

    private suspend fun fetchNumCtx(model: String): Int {
        val response: JsonObject = client.post("$ollamaUrl/api/show") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("name" to model))
        }.body()
        return response["modelinfo"]
            ?.jsonObject?.get("llama.context_length")
            ?.jsonPrimitive?.int ?: DEFAULT_NUM_CTX
    }
}
