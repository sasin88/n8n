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
