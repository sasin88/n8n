package com.nutriai.domain.ai

import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.result.AppError
import com.nutriai.domain.result.AppResult

/**
 * Valida la respuesta de cualquier proveedor de IA antes de mostrarla o guardarla.
 * Descarta lo que no tenga sentido en lugar de intentar adivinarlo.
 */
object FoodAnalysisValidator {
    const val LOW_CONFIDENCE = 0.6
    const val MAX_ITEMS = 20
    const val MAX_QUANTITY = 5000.0
    const val MAX_NAME_LENGTH = 80

    fun validate(raw: RawFoodAnalysis, providerName: String, isMock: Boolean): AppResult<FoodAnalysis> {
        val items = raw.items.orEmpty()
            .take(MAX_ITEMS)
            .mapNotNull(::validateItem)
        if (items.isEmpty()) return AppResult.Failure(AppError.InvalidAiResponse)

        val overall = raw.overallConfidence
            ?.takeIf { it.isValidConfidence() }
            ?: items.minOf { it.confidence }

        return AppResult.Success(
            FoodAnalysis(items = items, overallConfidence = overall, providerName = providerName, isMock = isMock),
        )
    }

    private fun validateItem(raw: RawDetectedFood): DetectedFood? {
        val name = raw.foodName?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_NAME_LENGTH } ?: return null
        val confidence = raw.confidence?.takeIf { it.isValidConfidence() } ?: return null
        val unit = parseUnit(raw.unit)
        val quantity = raw.estimatedQuantity?.takeIf { it > 0 && it <= MAX_QUANTITY && !it.isNaN() }
        // Sin unidad reconocida la cantidad no significa nada: se pedirá al usuario.
        return DetectedFood(
            name = name,
            estimatedQuantity = if (unit != null) quantity else null,
            unit = if (quantity != null) unit else null,
            confidence = confidence,
        )
    }

    private fun Double.isValidConfidence() = !isNaN() && this in 0.0..1.0

    fun parseUnit(value: String?): PortionUnit? {
        val key = value?.trim()?.lowercase() ?: return null
        return UNIT_ALIASES.entries.firstOrNull { (_, aliases) -> key in aliases }?.key
    }

    private val UNIT_ALIASES: Map<PortionUnit, Set<String>> = mapOf(
        PortionUnit.GRAM to setOf("g", "gr", "gram", "grams", "gramo", "gramos"),
        PortionUnit.MILLILITER to setOf("ml", "milliliter", "milliliters", "mililitro", "mililitros"),
        PortionUnit.UNIT to setOf("unit", "units", "unidad", "unidades"),
        PortionUnit.CUP to setOf("cup", "cups", "taza", "tazas"),
        PortionUnit.TABLESPOON to setOf("tbsp", "tablespoon", "tablespoons", "cucharada", "cucharadas"),
        PortionUnit.TEASPOON to setOf("tsp", "teaspoon", "teaspoons", "cucharadita", "cucharaditas"),
        PortionUnit.SERVING to setOf("serving", "servings", "portion", "porción", "porcion", "porciones"),
        PortionUnit.SLICE to setOf("slice", "slices", "rebanada", "rebanadas"),
        PortionUnit.PIECE to setOf("piece", "pieces", "pieza", "piezas"),
    )
}
