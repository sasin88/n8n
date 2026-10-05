package com.nutriai.data.repository

import com.nutriai.data.local.ProfileDao
import com.nutriai.data.settings.SettingsRepository
import com.nutriai.domain.model.UserProfile
import com.nutriai.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val dao: ProfileDao,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : ProfileRepository {

    override fun observeProfiles(): Flow<List<UserProfile>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveProfile(): Flow<UserProfile?> =
        settings.settings
            .map { it.activeProfileId }
            .flatMapLatest { id -> if (id == null) flowOf(null) else dao.observe(id).map { it?.toDomain() } }

    override suspend fun getProfile(id: Long): UserProfile? = dao.get(id)?.toDomain()

    override suspend fun create(profile: UserProfile): Long {
        val id = dao.insert(profile.copy(id = 0).toEntity(clock.nowEpochMs()))
        settings.setActiveProfile(id)
        return id
    }

    override suspend fun update(profile: UserProfile) {
        val existing = dao.get(profile.id) ?: return
        dao.update(profile.toEntity(existing.createdAtEpochMs))
    }

    override suspend fun delete(profileId: Long) {
        // Las claves foráneas en cascada eliminan comidas, peso y alimentos propios del perfil.
        dao.delete(profileId)
        if (settings.current().activeProfileId == profileId) {
            settings.setActiveProfile(dao.firstId())
        }
    }

    override suspend fun setActive(profileId: Long) {
        if (dao.get(profileId) != null) settings.setActiveProfile(profileId)
    }
}
