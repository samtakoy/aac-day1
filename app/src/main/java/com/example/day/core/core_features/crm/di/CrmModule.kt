package com.example.day.core.core_features.crm.di

import com.example.day.core.core_features.crm.data.CrmRepositoryImpl
import com.example.day.core.core_features.crm.domain.CrmRepository
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
internal interface CrmModule {
    @Binds
    @Singleton
    fun bindCrmRepository(impl: CrmRepositoryImpl): CrmRepository
}
