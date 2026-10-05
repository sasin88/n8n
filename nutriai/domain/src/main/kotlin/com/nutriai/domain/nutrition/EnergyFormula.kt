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

/** Mifflin-St Jeor (1990). Fórmula por defecto. */
object MifflinStJeorFormula : EnergyFormula {
    override val id = "mifflin_st_jeor"

    override fun basalMetabolicRate(metrics: BodyMetrics): Double {
        val base = 10.0 * metrics.weightKg + 6.25 * metrics.heightCm - 5.0 * metrics.ageYears
        return base + if (metrics.sex == Sex.MALE) 5.0 else -161.0
    }
}

/** Harris-Benedict revisada por Roza y Shizgal (1984). Alternativa disponible. */
object RevisedHarrisBenedictFormula : EnergyFormula {
    override val id = "harris_benedict_revised"

    override fun basalMetabolicRate(metrics: BodyMetrics): Double = with(metrics) {
        when (sex) {
            Sex.MALE -> 88.362 + 13.397 * weightKg + 4.799 * heightCm - 5.677 * ageYears
            Sex.FEMALE -> 447.593 + 9.247 * weightKg + 3.098 * heightCm - 4.330 * ageYears
        }
    }
}

object EnergyFormulas {
    val all: List<EnergyFormula> = listOf(MifflinStJeorFormula, RevisedHarrisBenedictFormula)
    val default: EnergyFormula = MifflinStJeorFormula

    fun byId(id: String?): EnergyFormula = all.firstOrNull { it.id == id } ?: default
}
