package com.nutriai.feature.meal

import com.nutriai.domain.ai.DetectedFood
import com.nutriai.domain.ai.FoodMatcher
import com.nutriai.domain.model.FoodReference
import com.nutriai.domain.model.Meal
import com.nutriai.domain.model.MealItem
import com.nutriai.domain.model.MealOrigin
import com.nutriai.domain.model.MealType
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.nutrition.PortionConverter
import java.time.LocalDate

/**
 * Línea en edición. Solo es guardable cuando tiene alimento de la base, cantidad convertible y,
 * si la IA no estaba segura, la confirmación del usuario.
 */
data class DraftItem(
    val key: Long,
    val food: FoodReference?,
    val quantity: Double?,
    val unit: PortionUnit?,
    /** Nombre que devolvió la IA (para mostrar qué creyó ver). */
    val detectedName: String? = null,
    val aiConfidence: Double? = null,
    /** La IA no estaba segura o no hubo coincidencia clara: requiere revisión. */
    val needsReview: Boolean = false,
    /** Línea ya guardada cuyo alimento ya no existe en la base: se conserva tal cual. */
    val frozen: MealItem? = null,
) {
    val mealItem: MealItem?
        get() = frozen ?: food?.let { f ->
            if (quantity != null && unit != null) PortionConverter.buildItem(f, quantity, unit, aiConfidence) else null
        }

    val displayName: String get() = food?.name ?: frozen?.name ?: detectedName ?: "Alimento"
    val isComplete: Boolean get() = mealItem != null && !needsReview
}

data class MealDraft(
    val mealId: Long = 0,
    val type: MealType,
    val date: LocalDate,
    val origin: MealOrigin,
    val items: List<DraftItem> = emptyList(),
    val analysisConfidence: Double? = null,
    val createdAtEpochMs: Long? = null,
) {
    val total: Nutrients get() = Nutrients.sum(items.mapNotNull { it.mealItem?.nutrients })
    val pendingCount: Int get() = items.count { !it.isComplete }
    val canSave: Boolean get() = items.isNotEmpty() && pendingCount == 0

    fun toMeal(profileId: Long, nowEpochMs: Long): Meal? {
        if (!canSave) return null
        return Meal(
            id = mealId,
            profileId = profileId,
            date = date,
            type = type,
            items = items.mapNotNull { it.mealItem },
            origin = origin,
            analysisConfidence = analysisConfidence,
            createdAtEpochMs = createdAtEpochMs ?: nowEpochMs,
        )
    }

    companion object {
        /** Coincidencia mínima para aceptar sin revisión el alimento propuesto. */
        const val CONFIDENT_MATCH = 0.85

        /** Convierte lo detectado por la IA en líneas, buscando cada alimento en la base nutricional. */
        fun fromDetected(detected: List<DetectedFood>, catalog: List<FoodReference>, nextKey: () -> Long): List<DraftItem> =
            detected.map { d ->
                val match = FoodMatcher.bestMatch(d.name, catalog)
                val food = match?.food
                val unitOk = food != null && d.unit != null && d.estimatedQuantity != null &&
                    PortionConverter.toBaseAmount(d.estimatedQuantity, d.unit, food) != null
                DraftItem(
                    key = nextKey(),
                    food = food,
                    quantity = if (unitOk) d.estimatedQuantity else null,
                    unit = if (unitOk) d.unit else food?.let { defaultUnit(it) },
                    detectedName = d.name,
                    aiConfidence = d.confidence,
                    needsReview = d.isUncertain || food == null || (match?.score ?: 0.0) < CONFIDENT_MATCH,
                )
            }

        fun defaultUnit(food: FoodReference): PortionUnit {
            val supported = PortionConverter.supportedUnits(food)
            return when {
                food.isRecipe && PortionUnit.SERVING in supported -> PortionUnit.SERVING
                PortionUnit.GRAM in supported -> PortionUnit.GRAM
                else -> supported.firstOrNull() ?: PortionUnit.GRAM
            }
        }

        /** Sustituye un plato compuesto por sus ingredientes, manteniendo la proporción. */
        fun expandRecipe(item: DraftItem, nextKey: () -> Long): List<DraftItem>? {
            val food = item.food ?: return null
            val base = item.mealItem?.baseAmount ?: return null
            if (!food.isRecipe) return null
            val total = food.components.sumOf { it.baseAmount }
            if (total <= 0) return null
            val factor = base / total
            return food.components.map { c ->
                DraftItem(
                    key = nextKey(),
                    food = c.food,
                    quantity = (c.baseAmount * factor).let { Math.round(it * 10) / 10.0 },
                    unit = PortionUnit.GRAM,
                    aiConfidence = item.aiConfidence,
                )
            }
        }
    }
}
