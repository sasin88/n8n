package com.nutriai.domain.model

/** Base sobre la que se expresan los nutrientes de referencia de un alimento. */
enum class NutrientBasis { PER_100_G, PER_100_ML }

/** Origen de los datos nutricionales, para poder mostrar y auditar la fuente. */
enum class NutritionSource {
    USDA_FDC,
    OPEN_FOOD_FACTS,
    USER_ENTERED,
    /** Plato compuesto calculado a partir de sus componentes. */
    RECIPE,
}

data class FoodReference(
    val id: String,
    val name: String,
    val nutrientsPerBasis: Nutrients,
    val basis: NutrientBasis,
    val source: NutritionSource,
    /** Identificador o descripción en la fuente original (p. ej. "FDC 168878 · Rice, white, ..."). */
    val sourceReference: String? = null,
    val portions: List<PortionEquivalent> = emptyList(),
    /** Sinónimos para la búsqueda y para emparejar nombres devueltos por la IA. */
    val aliases: List<String> = emptyList(),
    val category: FoodCategory = FoodCategory.OTHER,
    val countryCode: String? = null,
    val components: List<RecipeComponent> = emptyList(),
) {
    val isRecipe: Boolean get() = components.isNotEmpty()
}

data class RecipeComponent(
    val food: FoodReference,
    /** Cantidad del componente en la base de ese alimento (g o ml). */
    val baseAmount: Double,
)

enum class FoodCategory { GRAINS, LEGUMES, MEAT, FISH, DAIRY, EGGS, FRUIT, VEGETABLES, FATS, DRINKS, SWEETS, DISHES, OTHER }

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }
