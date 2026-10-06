package com.nutriai.domain.repository

import com.nutriai.domain.model.FoodReference
import com.nutriai.domain.model.Meal
import com.nutriai.domain.model.UserProfile
import com.nutriai.domain.model.WeightRecord
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfiles(): Flow<List<UserProfile>>

    /** Perfil activo, o null si no hay ninguno. */
    fun observeActiveProfile(): Flow<UserProfile?>

    suspend fun getProfile(id: Long): UserProfile?

    /** Crea el perfil y lo deja activo. Devuelve su id. */
    suspend fun create(profile: UserProfile): Long

    suspend fun update(profile: UserProfile)

    /** Elimina el perfil y todos sus datos asociados. */
    suspend fun delete(profileId: Long)

    suspend fun setActive(profileId: Long)
}

interface MealRepository {
    fun observeMeals(profileId: Long, from: LocalDate, to: LocalDate): Flow<List<Meal>>

    suspend fun getMeal(mealId: Long): Meal?

    /** Inserta o reemplaza la comida con sus líneas. Devuelve su id. */
    suspend fun save(meal: Meal): Long

    suspend fun delete(mealId: Long)

    suspend fun deleteAllForProfile(profileId: Long)
}

interface WeightRepository {
    fun observeRecords(profileId: Long): Flow<List<WeightRecord>>

    suspend fun add(record: WeightRecord): Long

    suspend fun delete(recordId: Long)
}

interface FoodRepository {
    suspend fun search(query: String, limit: Int = 30): List<FoodReference>

    suspend fun getFood(id: String): FoodReference?

    suspend fun allFoods(): List<FoodReference>

    /** Guarda un alimento introducido por el usuario. Devuelve su id. */
    suspend fun saveCustomFood(food: FoodReference): String
}

interface WaterRepository {
    fun observe(profileId: Long, from: LocalDate, to: LocalDate): Flow<List<com.nutriai.domain.model.WaterLog>>

    suspend fun add(log: com.nutriai.domain.model.WaterLog): Long

    /** Elimina el último registro del día (deshacer una taza). */
    suspend fun removeLast(profileId: Long, date: LocalDate)
}

interface ExerciseRepository {
    fun observe(profileId: Long, from: LocalDate, to: LocalDate): Flow<List<com.nutriai.domain.model.ExerciseLog>>

    suspend fun add(log: com.nutriai.domain.model.ExerciseLog): Long

    suspend fun delete(id: Long)
}

interface FastingRepository {
    fun observeActive(profileId: Long): Flow<com.nutriai.domain.activity.FastingSession?>

    fun observeHistory(profileId: Long, limit: Int = 30): Flow<List<com.nutriai.domain.activity.FastingSession>>

    suspend fun start(session: com.nutriai.domain.activity.FastingSession): Long

    suspend fun finish(sessionId: Long, endEpochMs: Long)

    suspend fun delete(sessionId: Long)
}

interface RecipeRepository {
    /** Guarda una receta propia a partir de sus ingredientes. Devuelve su id. */
    suspend fun saveUserRecipe(name: String, components: List<com.nutriai.domain.model.RecipeComponent>, tags: Set<com.nutriai.domain.model.FoodTag>): String

    suspend fun deleteUserRecipe(id: String)
}
