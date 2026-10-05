package com.nutriai.domain.model

enum class PortionUnit {
    GRAM,
    MILLILITER,
    UNIT,
    CUP,
    TABLESPOON,
    TEASPOON,
    SERVING,
    SLICE,
    PIECE,
}

/**
 * Equivalencia de una unidad casera a gramos para un alimento concreto
 * (una taza de arroz no pesa lo mismo que una taza de frijoles).
 */
data class PortionEquivalent(
    val unit: PortionUnit,
    val grams: Double,
    val label: String,
) {
    init {
        require(grams > 0) { "La equivalencia en gramos debe ser positiva" }
    }
}
