package com.nutriai.domain.repository

import com.nutriai.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfiles(): Flow<List<UserProfile>>

    fun observeActiveProfile(): Flow<UserProfile?>

    suspend fun create(profile: UserProfile): Long

    suspend fun update(profile: UserProfile)

    /** Elimina el perfil y todos sus datos asociados. */
    suspend fun delete(profileId: Long)

    suspend fun setActive(profileId: Long)
}
