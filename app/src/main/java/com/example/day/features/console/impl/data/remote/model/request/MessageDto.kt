package com.example.day.features.console.impl.data.remote.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: String
)