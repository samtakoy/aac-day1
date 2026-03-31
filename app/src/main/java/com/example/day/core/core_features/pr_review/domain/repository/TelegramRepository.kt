package com.example.day.core.core_features.pr_review.domain.repository

import com.example.day.core.core_features.pr_review.domain.model.TelegramPrEvent

interface TelegramRepository {
    suspend fun getPrUpdates(offset: Long): Result<List<TelegramPrEvent>>
}
