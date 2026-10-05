package com.nutriai.data.repository

import com.nutriai.data.local.MealDao
import com.nutriai.domain.model.Meal
import com.nutriai.domain.repository.MealRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class MealRepositoryImpl @Inject constructor(private val dao: MealDao) : MealRepository {

    override fun observeMeals(profileId: Long, from: LocalDate, to: LocalDate): Flow<List<Meal>> =
        combine(dao.observeMeals(profileId, from, to), dao.observeItems(profileId, from, to)) { meals, items ->
            val byMeal = items.groupBy { it.mealId }
            meals.map { it.toDomain(byMeal[it.id].orEmpty()) }
        }

    override suspend fun getMeal(mealId: Long): Meal? {
        val meal = dao.getMeal(mealId) ?: return null
        return meal.toDomain(dao.getItems(mealId))
    }

    override suspend fun save(meal: Meal): Long {
        require(meal.items.isNotEmpty()) { "Una comida necesita al menos un alimento" }
        return dao.saveMeal(meal.toEntity(), meal.items.mapIndexed { index, item -> item.toEntity(meal.id, index) })
    }

    override suspend fun delete(mealId: Long) = dao.deleteMeal(mealId)

    override suspend fun deleteAllForProfile(profileId: Long) = dao.deleteAllForProfile(profileId)
}
