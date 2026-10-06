package com.nutriai.data.repository

import com.nutriai.data.local.ActivityDao
import com.nutriai.data.local.ExerciseLogEntity
import com.nutriai.data.local.FastingSessionEntity
import com.nutriai.data.local.FoodDao
import com.nutriai.data.local.FoodEntity
import com.nutriai.data.local.FoodPortionEntity
import com.nutriai.data.local.RecipeComponentEntity
import com.nutriai.data.local.WaterLogEntity
import com.nutriai.data.settings.SettingsRepository
import com.nutriai.domain.activity.FastingSession
import com.nutriai.domain.model.ExerciseLog
import com.nutriai.domain.model.FoodCategory
import com.nutriai.domain.model.FoodTag
import com.nutriai.domain.model.NutrientBasis
import com.nutriai.domain.model.NutritionSource
import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.model.RecipeComponent
import com.nutriai.domain.model.WaterLog
import com.nutriai.domain.nutrition.RecipeCalculator
import com.nutriai.domain.repository.ExerciseRepository
import com.nutriai.domain.repository.FastingRepository
import com.nutriai.domain.repository.RecipeRepository
import com.nutriai.domain.repository.WaterRepository
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class WaterRepositoryImpl @Inject constructor(private val dao: ActivityDao) : WaterRepository {
    override fun observe(profileId: Long, from: LocalDate, to: LocalDate): Flow<List<WaterLog>> =
        dao.observeWater(profileId, from, to).map { list -> list.map { it.toDomain() } }

    override suspend fun add(log: WaterLog): Long {
        require(log.ml in 1..5000) { "Cantidad de agua fuera de rango" }
        return dao.insertWater(WaterLogEntity(0, log.profileId, log.date, log.ml, log.createdAtEpochMs))
    }

    override suspend fun removeLast(profileId: Long, date: LocalDate) = dao.deleteLastWater(profileId, date)
}

@Singleton
class ExerciseRepositoryImpl @Inject constructor(private val dao: ActivityDao) : ExerciseRepository {
    override fun observe(profileId: Long, from: LocalDate, to: LocalDate): Flow<List<ExerciseLog>> =
        dao.observeExercise(profileId, from, to).map { list -> list.map { it.toDomain() } }

    override suspend fun add(log: ExerciseLog): Long {
        require(log.minutes in 1..1440 && log.kcal in 0..10_000) { "Ejercicio fuera de rango" }
        return dao.insertExercise(
            ExerciseLogEntity(0, log.profileId, log.date, log.type.name, log.minutes, log.kcal, log.estimated, log.note),
        )
    }

    override suspend fun delete(id: Long) = dao.deleteExercise(id)
}

@Singleton
class FastingRepositoryImpl @Inject constructor(private val dao: ActivityDao) : FastingRepository {
    override fun observeActive(profileId: Long): Flow<FastingSession?> = dao.observeActiveFast(profileId).map { it?.toDomain() }

    override fun observeHistory(profileId: Long, limit: Int): Flow<List<FastingSession>> =
        dao.observeFastHistory(profileId, limit).map { list -> list.map { it.toDomain() } }

    override suspend fun start(session: FastingSession): Long = dao.insertFast(
        FastingSessionEntity(0, session.profileId, session.protocol.name, session.startEpochMs, session.plannedEndEpochMs, null),
    )

    override suspend fun finish(sessionId: Long, endEpochMs: Long) = dao.finishFast(sessionId, endEpochMs)

    override suspend fun delete(sessionId: Long) = dao.deleteFast(sessionId)
}

@Singleton
class RecipeRepositoryImpl @Inject constructor(
    private val dao: FoodDao,
    private val settings: SettingsRepository,
) : RecipeRepository {
    override suspend fun saveUserRecipe(name: String, components: List<RecipeComponent>, tags: Set<FoodTag>): String {
        require(name.isNotBlank() && components.isNotEmpty()) { "La receta necesita nombre e ingredientes" }
        val profileId = settings.current().activeProfileId
        val id = "user_recipe_" + UUID.randomUUID()
        val per100 = RecipeCalculator.nutrientsPer100(components)
        val total = RecipeCalculator.totalAmount(components)
        dao.replaceCatalog(
            foods = listOf(
                FoodEntity(
                    id = id, name = name.trim(), aliases = "", category = FoodCategory.DISHES, basis = NutrientBasis.PER_100_G,
                    energyKcal = per100.energyKcal, proteinG = per100.proteinG, carbohydratesG = per100.carbohydratesG,
                    fatG = per100.fatG, fiberG = per100.fiberG, source = NutritionSource.RECIPE,
                    sourceReference = "Receta creada por ti; valores calculados con sus ingredientes",
                    countryCode = null, ownerProfileId = profileId, tags = tags.joinToString("|") { it.name },
                ),
            ),
            portions = listOf(FoodPortionEntity(id, PortionUnit.SERVING, total, "1 porción (receta completa)")),
            components = components.map { RecipeComponentEntity(id, it.food.id, it.baseAmount) },
        )
        return id
    }

    override suspend fun deleteUserRecipe(id: String) = dao.deleteUserFood(id)
}
