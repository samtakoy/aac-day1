package com.example.day.core.core_features.memory.domain.repository

import com.example.day.core.core_features.memory.domain.model.UserProfile

/**
 * Repository for managing user profiles and their bindings.
 * A profile encapsulates a named LTM memory group for the user.
 */
interface UserProfileRepository {

    /**
     * Creates a new profile with the given name.
     * Internally creates an LTM group and links it to the profile.
     */
    suspend fun createProfile(name: String): UserProfile

    /**
     * Finds a profile by its title. Returns null if not found.
     */
    suspend fun getByName(name: String): UserProfile?

    /**
     * Gets a profile by its ID. Returns null if not found.
     */
    suspend fun getById(id: Long): UserProfile?

    /**
     * Deletes a profile by name. Also deletes its LTM group (cascade).
     */
    suspend fun deleteByName(name: String)

    /**
     * Binds the given profile to a user (upsert — replaces any existing binding).
     */
    suspend fun bindToUser(userId: Long, profileId: Long)

    /**
     * Removes the profile binding for the given user.
     * Does not delete the profile itself.
     */
    suspend fun unbindFromUser(userId: Long)

    /**
     * Returns the profile currently bound to the user, or null if none.
     */
    suspend fun getProfileForUser(userId: Long): UserProfile?

    /**
     * Updates the text avatar field of the given profile.
     * Pass null to reset the avatar.
     */
    suspend fun updateTextAvatar(profileId: Long, avatar: String?)
}
