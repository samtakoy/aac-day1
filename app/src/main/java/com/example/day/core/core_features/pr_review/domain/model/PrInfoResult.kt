package com.example.day.core.core_features.pr_review.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PrInfoResult(
    val title: String,
    val description: String,
    val headSha: String,
    val files: List<PrFileInfo>
)

@Serializable
data class PrFileInfo(
    val path: String,
    val status: String  // "added", "modified", "removed", "renamed"
)
