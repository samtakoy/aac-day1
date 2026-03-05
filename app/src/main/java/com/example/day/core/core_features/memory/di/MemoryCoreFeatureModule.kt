package com.example.day.core.core_features.memory.di

import com.example.day.core.core_features.chat.data.local.ChatDatabase
import com.example.day.core.core_features.memory.data.local.dao.ArtifactDao
import com.example.day.core.core_features.memory.data.local.dao.LTMGroupDao
import com.example.day.core.core_features.memory.data.local.dao.LongTermMemoryDao
import com.example.day.core.core_features.memory.data.repository.ArtifactRepositoryImpl
import com.example.day.core.core_features.memory.data.repository.LTMGroupRepositoryImpl
import com.example.day.core.core_features.memory.data.repository.LongTermMemoryRepositoryImpl
import com.example.day.core.core_features.memory.data.repository.UserProfileRepositoryImpl
import com.example.day.core.core_features.memory.domain.repository.ArtifactRepository
import com.example.day.core.core_features.memory.domain.repository.LTMGroupRepository
import com.example.day.core.core_features.memory.domain.repository.LongTermMemoryRepository
import com.example.day.core.core_features.memory.domain.repository.UserProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module
internal interface MemoryCoreFeatureModule {
    @Binds
    fun bindsLongTermMemoryRepository(impl: LongTermMemoryRepositoryImpl): LongTermMemoryRepository

    @Binds
    fun bindsArtifactRepository(impl: ArtifactRepositoryImpl): ArtifactRepository

    @Binds
    fun bindsLTMGroupRepository(impl: LTMGroupRepositoryImpl): LTMGroupRepository

    @Binds
    fun bindsUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository

    companion object {
        @Provides
        internal fun provideLongTermMemoryDao(db: ChatDatabase): LongTermMemoryDao = db.longTermMemoryDao()

        @Provides
        internal fun provideArtifactDao(db: ChatDatabase): ArtifactDao = db.artifactDao()

        @Provides
        internal fun provideLTMGroupDao(db: ChatDatabase): LTMGroupDao = db.ltmGroupDao()
    }
}
