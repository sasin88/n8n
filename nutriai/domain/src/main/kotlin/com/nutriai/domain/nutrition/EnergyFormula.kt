package com.nutriai.domain.nutrition

import com.nutriai.domain.model.Sex

/** Fórmula de tasa metabólica basal. Intercambiable para poder cambiar de método en el futuro. */
interface EnergyFormula {
    val id: String

    /** BMR estimado en kcal/día. */
    fun basalMetabolicRate(metrics: BodyMetrics): Double
}

data class BodyMetrics(
    val sex: Sex,
    val ageYears: Int,
    val weightKg: Double,
    val heightCm: Double,
)
