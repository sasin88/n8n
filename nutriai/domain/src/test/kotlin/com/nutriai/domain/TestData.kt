package com.nutriai.domain

import com.nutriai.domain.model.ActivityLevel
import com.nutriai.domain.model.FoodReference
import com.nutriai.domain.model.Goal
import com.nutriai.domain.model.NutrientBasis
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.model.NutritionSource
import com.nutriai.domain.model.PortionEquivalent
import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.model.Sex
import com.nutriai.domain.model.UserProfile

/** Datos ficticios solo para pruebas; no son valores nutricionales de referencia. */
object TestData {
    val adrian = UserProfile(
        name = "Adrian", sex = Sex.MALE, ageYears = 35, weightKg = 80.0, heightCm = 175.0,
        activityLevel = ActivityLevel.MODERATE, goal = Goal.LOSE_WEIGHT,
    )
    val maria = UserProfile(
        name = "María", sex = Sex.FEMALE, ageYears = 30, weightKg = 65.0, heightCm = 165.0,
        activityLevel = ActivityLevel.LIGHT, goal = Goal.MAINTAIN_WEIGHT,
    )

    fun food(
        id: String,
        name: String,
        per100: Nutrients = Nutrients(100.0, 10.0, 10.0, 1.0, 1.0),
        portions: List<PortionEquivalent> = emptyList(),
        aliases: List<String> = emptyList(),
    ) = FoodReference(
        id = id, name = name, nutrientsPerBasis = per100, basis = NutrientBasis.PER_100_G,
        source = NutritionSource.USDA_FDC, portions = portions, aliases = aliases,
    )

    val rice = food(
        "rice", "Arroz blanco cocido",
        portions = listOf(PortionEquivalent(PortionUnit.CUP, 158.0, "1 taza")),
        aliases = listOf("arroz", "rice"),
    )
    val chicken = food("chicken", "Pechuga de pollo asada", aliases = listOf("pollo", "chicken breast"))
    val beans = food("beans", "Frijoles negros cocidos", aliases = listOf("frijoles", "black beans"))
}
