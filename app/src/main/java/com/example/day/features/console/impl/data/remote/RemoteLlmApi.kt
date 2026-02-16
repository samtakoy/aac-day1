package com.example.day.features.console.impl.data.remote

internal interface RemoteLlmApi {
    suspend fun sendRequest(
        prompt: String,
        modelName: String = DEFAULT_MODEL
    ): Result<String>

    companion object {
        const val DEFAULT_MODEL = "z-ai/glm-4.5-air:free"
    }
}