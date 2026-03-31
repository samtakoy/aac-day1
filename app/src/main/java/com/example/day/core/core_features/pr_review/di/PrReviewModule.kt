package com.example.day.core.core_features.pr_review.di

import com.example.day.core.core_features.pr_review.data.PrHandleRepositoryImpl
import com.example.day.core.core_features.pr_review.data.TelegramRepositoryImpl
import com.example.day.core.core_features.pr_review.domain.repository.PrHandleRepository
import com.example.day.core.core_features.pr_review.domain.repository.TelegramRepository
import com.example.day.core.core_features.pr_review.domain.usecase.StartPrReviewUseCase
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
internal interface PrReviewModule {
    @Binds
    @Singleton
    fun bindPrHandleRepository(impl: PrHandleRepositoryImpl): PrHandleRepository

    @Binds
    @Singleton
    fun bindTelegramRepository(impl: TelegramRepositoryImpl): TelegramRepository
}
