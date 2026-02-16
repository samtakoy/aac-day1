package com.example.day.features.console.impl.data.remote

internal interface RemoteLlmApi {
    fun sendRequest(text: String): Result<String>
}