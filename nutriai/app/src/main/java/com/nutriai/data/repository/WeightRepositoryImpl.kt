package com.nutriai.data.repository

import com.nutriai.data.local.ProfileDao
import com.nutriai.data.local.WeightDao
import com.nutriai.domain.model.WeightRecord
import com.nutriai.domain.repository.WeightRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class WeightRepositoryImpl @Inject constructor(
    private val dao: WeightDao,
    private val profileDao: ProfileDao,
) : WeightRepository {

    override fun observeRecords(profileId: Long): Flow<List<WeightRecord>> =
        dao.observe(profileId).map { list -> list.map { it.toDomain() } }

    /** Guarda el registro y actualiza el peso actual del perfil para recalcular su objetivo. */
    override suspend fun add(record: WeightRecord): Long {
        require(record.weightKg in 20.0..400.0) { "Peso fuera de rango" }
        val id = dao.insert(record.toEntity())
        profileDao.get(record.profileId)?.let { profileDao.update(it.copy(weightKg = record.weightKg)) }
        return id
    }

    override suspend fun delete(recordId: Long) = dao.delete(recordId)
}
