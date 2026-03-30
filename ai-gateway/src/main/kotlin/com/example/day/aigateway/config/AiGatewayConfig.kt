package com.example.day.aigateway.config

data class AiGatewayConfig(
    val ollamaUrl: String = System.getenv("OLLAMA_URL") ?: "http://localhost:11434",
    val port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8081,
    val rateLimitRpm: Int = System.getenv("RATE_LIMIT_RPM")?.toIntOrNull() ?: 60,
    val concurrencyLimit: Int = System.getenv("CONCURRENCY_LIMIT")?.toIntOrNull() ?: 4,
    val ollamaMaxContext: Int = System.getenv("OLLAMA_MAX_CONTEXT")?.toIntOrNull() ?: 8192
) {
    init {
        require(port in 1..65535) { "PORT must be 1-65535, got $port" }
        require(rateLimitRpm > 0) { "RATE_LIMIT_RPM must be > 0, got $rateLimitRpm" }
        require(concurrencyLimit > 0) { "CONCURRENCY_LIMIT must be > 0, got $concurrencyLimit" }
        require(ollamaMaxContext > 0) { "OLLAMA_MAX_CONTEXT must be > 0, got $ollamaMaxContext" }
    }
}
