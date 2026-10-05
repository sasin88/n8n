package com.nutriai.domain.model

import java.time.LocalDate

enum class MealOrigin { MANUAL, AI_PHOTO }

data class MealItem(
    val id: Long = 0,
    /** null cuando el alimento no está en el catálogo (p. ej. introducido a mano). */
    val foodId: String?,
    val name: String,
    val quantity: Double,
    val unit: PortionUnit,
    /** Cantidad convertida a la base del alimento (g o ml), usada para el cálculo. */
    val baseAmount: Double,
    val nutrients: Nutrients,
    val source: NutritionSource,
    /** Confianza de la IA para este alimento (0–1), si vino de un análisis. */
    val aiConfidence: Double? = null,
)

data class Meal(
    val id: Long = 0,
    val profileId: Long,
    val date: LocalDate,
    val type: MealType,
    val items: List<MealItem>,
    val origin: MealOrigin,
    val analysisConfidence: Double? = null,
    val createdAtEpochMs: Long,
) {
    val nutrients: Nutrients get() = Nutrients.sum(items.map { it.nutrients })
}

data class WeightRecord(
    val id: Long = 0,
    val profileId: Long,
    val date: LocalDate,
    val weightKg: Double,
)

data class DailySummary(
    val date: LocalDate,
    val nutrients: Nutrients,
    val mealCount: Int,
)
