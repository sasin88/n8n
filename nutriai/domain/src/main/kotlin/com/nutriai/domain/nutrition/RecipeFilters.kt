package com.nutriai.domain.nutrition

import com.nutriai.domain.model.FoodReference
import com.nutriai.domain.model.FoodTag
import com.nutriai.domain.model.PortionUnit

/** Rangos de calorías por porción para explorar recetas. */
enum class CalorieRange(val min: Int, val max: Int?) {
    UP_TO_200(0, 200), R200_300(200, 300), R300_400(300, 400), R400_500(400, 500), R500_600(500, 600), OVER_600(600, null);

    fun contains(kcal: Double) = kcal >= min && (max == null || kcal < max)
}

object RecipeFilters {
    /** kcal de una porción de la receta (su porción definida o, si no hay, 100 g). */
    fun kcalPerServing(recipe: FoodReference): Double {
        val grams = recipe.portions.firstOrNull { it.unit == PortionUnit.SERVING }?.grams ?: 100.0
        return recipe.nutrientsPerBasis.energyKcal * grams / 100.0
    }

    fun filter(recipes: List<FoodReference>, range: CalorieRange?, tags: Set<FoodTag>, query: String = ""): List<FoodReference> {
        val q = query.trim().lowercase()
        return recipes.filter { r ->
            r.isRecipe &&
                (range == null || range.contains(kcalPerServing(r))) &&
                r.tags.containsAll(tags) &&
                (q.isEmpty() || r.name.lowercase().contains(q) || r.aliases.any { it.lowercase().contains(q) })
        }
    }
}
