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
    @SerialName("used_by") val usedBy: List<String> = emptyList(),
)

/**
 * Строит составной текст для embedding — включает все семантически значимые поля.
 * Используется вместо одного поля responsibility для более богатого семантического индекса.
 */
fun ClassMetadata.toEmbeddingText(): String = buildString {
    append(responsibility)
    if (domainTags.isNotEmpty())
        append(". ${domainTags.joinToString(", ")}")
    if (keyMethods.isNotEmpty())
        append(". " + keyMethods.joinToString(". ") { "${it.name}: ${it.description}" })
    if (usedBy.isNotEmpty())
        append(". Used by: ${usedBy.joinToString(", ")}")
}

@Serializable
data class MethodInfo(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
    @SerialName("params") val params: String? = null,
)
