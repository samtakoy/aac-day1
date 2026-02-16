package com.example.day.features.console.impl.data.remote

import android.util.Log
import io.ktor.client.HttpClient
import javax.inject.Inject

internal class RemoteLlmApiImpl @Inject constructor(
    private val client: HttpClient
) : RemoteLlmApi {
    override fun sendRequest(text: String): Result<String> {
        TODO()
    }
}