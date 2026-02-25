package com.example.day.core.core_features.chat.domain.model

data class User(
    val id: Long,
    val name: String,
    val type: UserType,
    val avatar: String? = null
)
