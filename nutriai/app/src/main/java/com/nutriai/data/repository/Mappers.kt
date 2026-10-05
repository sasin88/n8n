package com.nutriai.data.repository

import com.nutriai.data.local.FoodEntity
import com.nutriai.data.local.FoodPortionEntity
import com.nutriai.data.local.MealEntity
import com.nutriai.data.local.MealItemEntity
import com.nutriai.data.local.ProfileEntity
import com.nutriai.data.local.RecipeComponentEntity
import com.nutriai.data.local.WeightRecordEntity
import com.nutriai.domain.model.FoodReference
import com.nutriai.domain.model.Meal
import com.nutriai.domain.model.MealItem
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.model.PortionEquivalent
import com.nutriai.domain.model.RecipeComponent
import com.nutriai.domain.model.UserProfile
import com.nutriai.domain.model.WeightRecord

internal fun ProfileEntity.toDomain() = UserProfile(
    id = id, name = name, sex = sex, ageYears = ageYears, weightKg = weightKg, heightCm = heightCm,
    activityLevel = activityLevel, goal = goal, targetWeightKg = targetWeightKg,
    manualCalorieTarget = manualCalorieTarget, unitSystem = unitSystem, countryCode = countryCode,
    languageTag = languageTag,
)

internal fun UserProfile.toEntity(createdAtEpochMs: Long) = ProfileEntity(
    id = id, name = name.trim(), sex = sex, ageYears = ageYears, weightKg = weightKg, heightCm = heightCm,
    activityLevel = activityLevel, goal = goal, targetWeightKg = targetWeightKg,
    manualCalorieTarget = manualCalorieTarget, unitSystem = unitSystem, countryCode = countryCode,
    languageTag = languageTag, createdAtEpochMs = createdAtEpochMs,
)

internal fun FoodEntity.nutrients() = Nutrients(energyKcal, proteinG, carbohydratesG, fatG, fiberG)

/** Versión sin porciones ni componentes, útil para cálculos de recetas. */
internal fun FoodEntity.toReferenceStub() = FoodReference(
    id = id, name = name, nutrientsPerBasis = nutrients(), basis = basis, source = source,
    sourceReference = sourceReference, aliases = aliasList(), category = category, countryCode = countryCode,
)

internal fun FoodEntity.aliasList(): List<String> = aliases.split('|').filter { it.isNotBlank() }

/** Ensambla los alimentos con sus porciones y, para las recetas, sus componentes. */
internal fun assembleFoods(
    foods: List<FoodEntity>,
    portions: List<FoodPortionEntity>,
    components: List<RecipeComponentEntity>,
): List<FoodReference> {
    val portionsByFood = portions.groupBy { it.foodId }
    val basic = foods.associate { food ->
        food.id to food.toReferenceStub().copy(
            portions = portionsByFood[food.id].orEmpty().map { PortionEquivalent(it.unit, it.grams, it.label) },
        )
    }
    val componentsByRecipe = components.groupBy { it.recipeId }
    return foods.map { food ->
        val base = basic.getValue(food.id)
        val parts = componentsByRecipe[food.id].orEmpty().mapNotNull { c ->
            basic[c.componentFoodId]?.let { RecipeComponent(it, c.baseAmount) }
        }
        if (parts.isEmpty()) base else base.copy(components = parts)
    }
}

internal fun FoodReference.toEntity(ownerProfileId: Long?) = FoodEntity(
    id = id, name = name.trim(), aliases = aliases.joinToString("|"), category = category, basis = basis,
    energyKcal = nutrientsPerBasis.energyKcal, proteinG = nutrientsPerBasis.proteinG,
    carbohydratesG = nutrientsPerBasis.carbohydratesG, fatG = nutrientsPerBasis.fatG, fiberG = nutrientsPerBasis.fiberG,
    source = source, sourceReference = sourceReference, countryCode = countryCode, ownerProfileId = ownerProfileId,
)

internal fun MealItemEntity.toDomain() = MealItem(
    id = id, foodId = foodId, name = name, quantity = quantity, unit = unit, baseAmount = baseAmount,
    nutrients = Nutrients(energyKcal, proteinG, carbohydratesG, fatG, fiberG), source = source, aiConfidence = aiConfidence,
)

internal fun MealItem.toEntity(mealId: Long, position: Int) = MealItemEntity(
    id = 0, mealId = mealId, position = position, foodId = foodId, name = name, quantity = quantity, unit = unit,
    baseAmount = baseAmount, energyKcal = nutrients.energyKcal, proteinG = nutrients.proteinG,
    carbohydratesG = nutrients.carbohydratesG, fatG = nutrients.fatG, fiberG = nutrients.fiberG,
    source = source, aiConfidence = aiConfidence,
)

internal fun MealEntity.toDomain(items: List<MealItemEntity>) = Meal(
    id = id, profileId = profileId, date = date, type = type, items = items.map { it.toDomain() },
    origin = origin, analysisConfidence = analysisConfidence, createdAtEpochMs = createdAtEpochMs,
)

internal fun Meal.toEntity() = MealEntity(
    id = id, profileId = profileId, date = date, type = type, origin = origin,
    analysisConfidence = analysisConfidence, createdAtEpochMs = createdAtEpochMs,
)

internal fun WeightRecordEntity.toDomain() = WeightRecord(id, profileId, date, weightKg)

internal fun WeightRecord.toEntity() = WeightRecordEntity(id, profileId, date, weightKg)
