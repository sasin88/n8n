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
    val sourceReference: String? = null,
    val portions: List<PortionEquivalent> = emptyList(),
)

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }
