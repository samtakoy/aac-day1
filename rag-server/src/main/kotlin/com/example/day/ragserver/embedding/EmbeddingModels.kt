package com.example.day.ragserver.embedding

import kotlinx.serialization.Serializable

@Serializable
data class OllamaEmbedRequest(val model: String, val prompt: String)

@Serializable
data class OllamaEmbedResponse(val embedding: List<Float>)

@Serializable
data class OpenRouterEmbedRequest(val model: String, val input: String)

@Serializable
data class OpenRouterEmbedResponse(val data: List<EmbedData>)

@Serializable
data class EmbedData(val embedding: List<Float>)
