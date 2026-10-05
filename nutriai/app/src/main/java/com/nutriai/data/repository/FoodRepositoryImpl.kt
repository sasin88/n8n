package com.nutriai.data.repository

import com.nutriai.data.local.FoodDao
import com.nutriai.data.settings.SettingsRepository
import com.nutriai.domain.ai.FoodMatcher
import com.nutriai.domain.model.FoodReference
import com.nutriai.domain.model.NutritionSource
import com.nutriai.domain.repository.FoodRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val dao: FoodDao,
    private val settings: SettingsRepository,
) : FoodRepository {

    override suspend fun allFoods(): List<FoodReference> {
        val profileId = settings.current().activeProfileId
        return assembleFoods(dao.visibleFoods(profileId), dao.allPortions(), dao.allRecipeComponents())
            .sortedBy { it.name }
    }

    override suspend fun search(query: String, limit: Int): List<FoodReference> {
        val all = allFoods()
        if (query.isBlank()) return all.take(limit)
        return FoodMatcher.rank(query, all, limit).map { it.food }
    }

    override suspend fun getFood(id: String): FoodReference? = allFoods().firstOrNull { it.id == id }

    override suspend fun saveCustomFood(food: FoodReference): String {
        val profileId = settings.current().activeProfileId
        val id = food.id.ifBlank { "user_" + UUID.randomUUID() }
        dao.upsertFoods(listOf(food.copy(id = id, source = NutritionSource.USER_ENTERED).toEntity(profileId)))
        return id
    }
}
