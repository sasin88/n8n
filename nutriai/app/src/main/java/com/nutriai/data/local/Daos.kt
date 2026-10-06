package com.nutriai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY createdAtEpochMs")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    fun observe(id: Long): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun get(id: Long): ProfileEntity?

    @Query("SELECT id FROM profiles ORDER BY createdAtEpochMs LIMIT 1")
    suspend fun firstId(): Long?

    @Insert
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods WHERE ownerProfileId IS NULL OR ownerProfileId = :profileId")
    suspend fun visibleFoods(profileId: Long?): List<FoodEntity>

    @Query("SELECT * FROM food_portions")
    suspend fun allPortions(): List<FoodPortionEntity>

    @Query("SELECT * FROM recipe_components")
    suspend fun allRecipeComponents(): List<RecipeComponentEntity>

    @Upsert
    suspend fun upsertFoods(foods: List<FoodEntity>)

    @Upsert
    suspend fun upsertPortions(portions: List<FoodPortionEntity>)

    @Upsert
    suspend fun upsertRecipeComponents(components: List<RecipeComponentEntity>)

    @Query("DELETE FROM food_portions WHERE foodId IN (:foodIds)")
    suspend fun deletePortionsFor(foodIds: List<String>)

    @Query("DELETE FROM foods WHERE id = :id AND ownerProfileId IS NOT NULL")
    suspend fun deleteUserFood(id: String)

    @Query("DELETE FROM recipe_components WHERE recipeId IN (:recipeIds)")
    suspend fun deleteComponentsFor(recipeIds: List<String>)

    @Transaction
    suspend fun replaceCatalog(foods: List<FoodEntity>, portions: List<FoodPortionEntity>, components: List<RecipeComponentEntity>) {
        upsertFoods(foods)
        val ids = foods.map { it.id }
        deletePortionsFor(ids)
        deleteComponentsFor(ids)
        upsertPortions(portions)
        upsertRecipeComponents(components)
    }
}

@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE profileId = :profileId AND date BETWEEN :from AND :to ORDER BY date, createdAtEpochMs")
    fun observeMeals(profileId: Long, from: LocalDate, to: LocalDate): Flow<List<MealEntity>>

    @Query(
        "SELECT meal_items.* FROM meal_items INNER JOIN meals ON meals.id = meal_items.mealId " +
            "WHERE meals.profileId = :profileId AND meals.date BETWEEN :from AND :to ORDER BY meal_items.position",
    )
    fun observeItems(profileId: Long, from: LocalDate, to: LocalDate): Flow<List<MealItemEntity>>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getMeal(id: Long): MealEntity?

    @Query("SELECT * FROM meal_items WHERE mealId = :mealId ORDER BY position")
    suspend fun getItems(mealId: Long): List<MealItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Insert
    suspend fun insertItems(items: List<MealItemEntity>)

    @Query("DELETE FROM meal_items WHERE mealId = :mealId")
    suspend fun deleteItems(mealId: Long)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteMeal(id: Long)

    @Query("DELETE FROM meals WHERE profileId = :profileId")
    suspend fun deleteAllForProfile(profileId: Long)

    @Transaction
    suspend fun saveMeal(meal: MealEntity, items: List<MealItemEntity>): Long {
        val id = if (meal.id == 0L) insertMeal(meal) else meal.id.also { deleteItems(it); insertMeal(meal) }
        insertItems(items.mapIndexed { index, item -> item.copy(id = 0, mealId = id, position = index) })
        return id
    }
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_records WHERE profileId = :profileId ORDER BY date, id")
    fun observe(profileId: Long): Flow<List<WeightRecordEntity>>

    @Insert
    suspend fun insert(record: WeightRecordEntity): Long

    @Query("DELETE FROM weight_records WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM water_logs WHERE profileId = :profileId AND date BETWEEN :from AND :to ORDER BY createdAtEpochMs")
    fun observeWater(profileId: Long, from: LocalDate, to: LocalDate): Flow<List<WaterLogEntity>>

    @Insert
    suspend fun insertWater(log: WaterLogEntity): Long

    @Query("DELETE FROM water_logs WHERE id = (SELECT id FROM water_logs WHERE profileId = :profileId AND date = :date ORDER BY createdAtEpochMs DESC LIMIT 1)")
    suspend fun deleteLastWater(profileId: Long, date: LocalDate)

    @Query("SELECT * FROM exercise_logs WHERE profileId = :profileId AND date BETWEEN :from AND :to ORDER BY id")
    fun observeExercise(profileId: Long, from: LocalDate, to: LocalDate): Flow<List<ExerciseLogEntity>>

    @Insert
    suspend fun insertExercise(log: ExerciseLogEntity): Long

    @Query("DELETE FROM exercise_logs WHERE id = :id")
    suspend fun deleteExercise(id: Long)

    @Query("SELECT * FROM fasting_sessions WHERE profileId = :profileId AND endEpochMs IS NULL ORDER BY startEpochMs DESC LIMIT 1")
    fun observeActiveFast(profileId: Long): Flow<FastingSessionEntity?>

    @Query("SELECT * FROM fasting_sessions WHERE profileId = :profileId AND endEpochMs IS NOT NULL ORDER BY startEpochMs DESC LIMIT :limit")
    fun observeFastHistory(profileId: Long, limit: Int): Flow<List<FastingSessionEntity>>

    @Insert
    suspend fun insertFast(session: FastingSessionEntity): Long

    @Query("UPDATE fasting_sessions SET endEpochMs = :endEpochMs WHERE id = :id")
    suspend fun finishFast(id: Long, endEpochMs: Long)

    @Query("DELETE FROM fasting_sessions WHERE id = :id")
    suspend fun deleteFast(id: Long)
}
