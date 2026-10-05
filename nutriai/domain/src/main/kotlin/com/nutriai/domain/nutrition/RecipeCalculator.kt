package com.nutriai.domain.nutrition

import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.model.RecipeComponent

/** Platos compuestos: el total es la suma de sus componentes. */
object RecipeCalculator {

    fun totalNutrients(components: List<RecipeComponent>): Nutrients =
        Nutrients.sum(components.map { it.food.nutrientsPerBasis.scaledBy(it.baseAmount / 100.0) })

    fun totalAmount(components: List<RecipeComponent>): Double = components.sumOf { it.baseAmount }

    /** Nutrientes por 100 g del plato completo. */
    fun nutrientsPer100(components: List<RecipeComponent>): Nutrients {
        val total = totalAmount(components)
        require(total > 0) { "Una receta necesita al menos un componente con cantidad" }
        return totalNutrients(components).scaledBy(100.0 / total)
    }
}
