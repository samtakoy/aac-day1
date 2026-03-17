package com.example.day.ragserver.db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClassMetadata(
    @SerialName("class_name") val className: String,
    @SerialName("responsibility") val responsibility: String,
    @SerialName("dependencies") val dependencies: List<String> = emptyList(),
    @SerialName("key_methods") val keyMethods: List<MethodInfo> = emptyList(),
    @SerialName("domain_tags") val domainTags: List<String> = emptyList(),
)

@Serializable
data class MethodInfo(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
)
