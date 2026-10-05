package com.nutriai.domain.nutrition

import com.nutriai.domain.model.FoodReference
import com.nutriai.domain.model.MealItem
import com.nutriai.domain.model.NutrientBasis
import com.nutriai.domain.model.PortionUnit

/**
 * Convierte cantidades caseras a la base del alimento (g o ml).
 *
 * Solo usa equivalencias conocidas del propio alimento y conversiones exactas entre medidas
 * de volumen estadounidenses (taza, cucharada, cucharadita, ml). Si no hay forma fiable de
 * convertir, devuelve null y la UI pide la cantidad en gramos.
 */
object PortionConverter {

    /** Mililitros por unidad de volumen (medidas US). */
    private val ML_PER_UNIT = mapOf(
        PortionUnit.MILLILITER to 1.0,
        PortionUnit.CUP to 236.5882,
        PortionUnit.TABLESPOON to 14.7868,
        PortionUnit.TEASPOON to 4.9289,
    )

    fun toBaseAmount(quantity: Double, unit: PortionUnit, food: FoodReference): Double? {
        if (quantity <= 0 || quantity.isNaN() || quantity.isInfinite()) return null
        val gramsPerUnit = gramsPerUnit(unit, food) ?: return null
        return quantity * gramsPerUnit
    }

    /** Unidades que se pueden usar con este alimento. */
    fun supportedUnits(food: FoodReference): List<PortionUnit> =
        PortionUnit.entries.filter { gramsPerUnit(it, food) != null }

    private fun gramsPerUnit(unit: PortionUnit, food: FoodReference): Double? {
        if (unit == PortionUnit.GRAM && food.basis == NutrientBasis.PER_100_G) return 1.0
        if (unit == PortionUnit.MILLILITER && food.basis == NutrientBasis.PER_100_ML) return 1.0
        food.portions.firstOrNull { it.unit == unit }?.let { return it.grams }

        // Derivar entre medidas de volumen a partir de cualquier equivalencia de volumen conocida.
        val targetMl = ML_PER_UNIT[unit] ?: return null
        val known = food.portions.firstOrNull { it.unit in ML_PER_UNIT } ?: return null
        val gramsPerMl = known.grams / ML_PER_UNIT.getValue(known.unit)
        return targetMl * gramsPerMl
    }

    /** Construye una línea de comida a partir de un alimento del catálogo. */
    fun buildItem(food: FoodReference, quantity: Double, unit: PortionUnit, aiConfidence: Double? = null): MealItem? {
        val base = toBaseAmount(quantity, unit, food) ?: return null
        return MealItem(
            foodId = food.id,
            name = food.name,
            quantity = quantity,
            unit = unit,
            baseAmount = base,
            nutrients = food.nutrientsPerBasis.scaledBy(base / 100.0),
            source = food.source,
            aiConfidence = aiConfidence,
        )
    }
}
