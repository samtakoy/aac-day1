package com.example.day.aigateway.config

data class AiGatewayConfig(
    val ollamaUrl: String = System.getenv("OLLAMA_URL") ?: "http://localhost:11434",
    val port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8081
)
