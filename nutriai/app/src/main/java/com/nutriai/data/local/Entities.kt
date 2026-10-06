package com.nutriai.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nutriai.domain.model.ActivityLevel
import com.nutriai.domain.model.FoodCategory
import com.nutriai.domain.model.Goal
import com.nutriai.domain.model.MealOrigin
import com.nutriai.domain.model.MealType
import com.nutriai.domain.model.NutrientBasis
import com.nutriai.domain.model.NutritionSource
import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.model.Sex
import com.nutriai.domain.model.UnitSystem
import java.time.LocalDate

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sex: Sex,
    val ageYears: Int,
    val weightKg: Double,
    val heightCm: Double,
    val activityLevel: ActivityLevel,
    val goal: Goal,
    val targetWeightKg: Double?,
    val manualCalorieTarget: Int?,
    val unitSystem: UnitSystem,
    val countryCode: String?,
    val languageTag: String?,
    val createdAtEpochMs: Long,
    /** Nombres de SecondaryGoal separados por '|'. */
    @ColumnInfo(defaultValue = "''") val secondaryGoals: String = "",
    val weeklyRateKg: Double? = null,
    val waterGoalMl: Int? = null,
    @ColumnInfo(defaultValue = "0") val exerciseAddsToBudget: Boolean = false,
)

/**
 * Alimento del catálogo (ownerProfileId = null) o creado por un perfil.
 * Los nutrientes se guardan por 100 g/ml junto con su fuente.
 */
@Entity(
    tableName = "foods",
    foreignKeys = [
        ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["ownerProfileId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("ownerProfileId")],
)
data class FoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val aliases: String,
    val category: FoodCategory,
    val basis: NutrientBasis,
    val energyKcal: Double,
    val proteinG: Double,
    val carbohydratesG: Double,
    val fatG: Double,
    val fiberG: Double?,
    val source: NutritionSource,
    val sourceReference: String?,
    val countryCode: String?,
    val ownerProfileId: Long?,
    /** Nombres de FoodTag separados por '|'. */
    @ColumnInfo(defaultValue = "''") val tags: String = "",
)

@Entity(
    tableName = "food_portions",
    primaryKeys = ["foodId", "unit"],
    foreignKeys = [
        ForeignKey(entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["foodId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class FoodPortionEntity(
    val foodId: String,
    val unit: PortionUnit,
    val grams: Double,
    val label: String,
)

@Entity(
    tableName = "recipe_components",
    primaryKeys = ["recipeId", "componentFoodId"],
    foreignKeys = [
        ForeignKey(entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["componentFoodId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("componentFoodId")],
)
data class RecipeComponentEntity(
    val recipeId: String,
    val componentFoodId: String,
    val baseAmount: Double,
)

@Entity(
    tableName = "meals",
    foreignKeys = [
        ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["profileId", "date"])],
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val date: LocalDate,
    val type: MealType,
    val origin: MealOrigin,
    val analysisConfidence: Double?,
    val createdAtEpochMs: Long,
)

/** Línea de comida. Guarda una copia de los nutrientes calculados en el momento del registro. */
@Entity(
    tableName = "meal_items",
    foreignKeys = [
        ForeignKey(entity = MealEntity::class, parentColumns = ["id"], childColumns = ["mealId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("mealId")],
)
data class MealItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val position: Int,
    val foodId: String?,
    val name: String,
    val quantity: Double,
    val unit: PortionUnit,
    val baseAmount: Double,
    val energyKcal: Double,
    val proteinG: Double,
    val carbohydratesG: Double,
    val fatG: Double,
    val fiberG: Double?,
    val source: NutritionSource,
    val aiConfidence: Double?,
)

@Entity(
    tableName = "weight_records",
    foreignKeys = [
        ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["profileId", "date"])],
)
data class WeightRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val date: LocalDate,
    val weightKg: Double,
)

@Entity(
    tableName = "water_logs",
    foreignKeys = [
        ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["profileId", "date"])],
)
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val date: LocalDate,
    val ml: Int,
    val createdAtEpochMs: Long,
)

@Entity(
    tableName = "exercise_logs",
    foreignKeys = [
        ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["profileId", "date"])],
)
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val date: LocalDate,
    val type: String,
    val minutes: Int,
    val kcal: Int,
    val estimated: Boolean,
    val note: String?,
)

@Entity(
    tableName = "fasting_sessions",
    foreignKeys = [
        ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("profileId")],
)
data class FastingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val protocol: String,
    val startEpochMs: Long,
    val plannedEndEpochMs: Long,
    val endEpochMs: Long?,
)
