package com.example.day.core.core_features.memory.data.repository

import com.example.day.core.core_features.memory.data.local.dao.UserProfileDao
import com.example.day.core.core_features.memory.data.local.model.link.UserToProfileEntity
import com.example.day.core.core_features.memory.data.local.model.user.UserProfileEntity
import com.example.day.core.core_features.memory.domain.model.UserProfile
import com.example.day.core.core_features.memory.domain.repository.LTMGroupRepository
import com.example.day.core.core_features.memory.domain.repository.UserProfileRepository
import javax.inject.Inject

internal class UserProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val ltmGroupRepository: LTMGroupRepository
) : UserProfileRepository {

    override suspend fun createProfile(name: String): UserProfile {
        val ltmGroupId = ltmGroupRepository.createGroup()
        val entity = UserProfileEntity(
            title = name,
            memoryGroupId = ltmGroupId
        )
        val profileId = userProfileDao.insertProfile(entity)
        return UserProfile(id = profileId, title = name, ltmGroupId = ltmGroupId, textAvatar = null)
    }

    override suspend fun getByName(name: String): UserProfile? {
        return userProfileDao.getByTitle(name)?.toDomain()
    }

    override suspend fun getById(id: Long): UserProfile? {
        return userProfileDao.getById(id)?.toDomain()
    }

    // TODO Transaction
    override suspend fun deleteByName(name: String) {
        val entity = userProfileDao.getByTitle(name) ?: return
        // Delete LTM group first (CASCADE will clean up LTM facts)
        ltmGroupRepository.deleteById(entity.memoryGroupId)
        // Profile deletion cascades from LTM group RESTRICT, so delete profile explicitly
        userProfileDao.deleteByTitle(name)
    }

    override suspend fun bindToUser(userId: Long, profileId: Long) {
        userProfileDao.upsertUserToProfile(UserToProfileEntity(userId = userId, profileId = profileId))
    }

    override suspend fun unbindFromUser(userId: Long) {
        userProfileDao.deleteUserToProfile(userId)
    }

    override suspend fun getProfileForUser(userId: Long): UserProfile? {
        return userProfileDao.getProfileForUser(userId)?.toDomain()
    }

    override suspend fun updateTextAvatar(profileId: Long, avatar: String?) {
        userProfileDao.updateTextAvatar(profileId, avatar)
    }

    override suspend fun getAllProfiles(): List<UserProfile> {
        return userProfileDao.getAllProfiles().map { it.toDomain() }
    }

    private fun UserProfileEntity.toDomain() = UserProfile(
        id = id,
        title = title,
        ltmGroupId = memoryGroupId,
        textAvatar = textAvatar
    )
}
