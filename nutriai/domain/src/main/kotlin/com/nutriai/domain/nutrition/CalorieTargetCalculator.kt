package com.nutriai.domain.nutrition

import com.nutriai.domain.health.WeightGoalPlanner
import com.nutriai.domain.model.Goal
import com.nutriai.domain.model.Sex
import com.nutriai.domain.model.UserProfile
import kotlin.math.roundToInt

data class MacroTargets(val proteinG: Double, val carbohydratesG: Double, val fatG: Double)

/**
 * Resultado del cálculo. Todos los valores son estimaciones orientativas, no una prescripción.
 */
data class CalorieTarget(
    val bmrKcal: Double,
    val tdeeKcal: Double,
    /** Objetivo calculado por la app según el objetivo del perfil. */
    val suggestedKcal: Int,
    /** Objetivo realmente usado: el manual del usuario si lo fijó, si no el sugerido. */
    val effectiveKcal: Int,
    val isManual: Boolean,
    /** true si el objetivo sugerido se subió al mínimo de seguridad. */
    val minimumApplied: Boolean,
    /** true si el objetivo manual está por debajo del mínimo de seguridad. */
    val manualBelowMinimum: Boolean,
    val macros: MacroTargets,
)

class CalorieTargetCalculator(private val formula: EnergyFormula = EnergyFormulas.default) {

    fun calculate(profile: UserProfile): CalorieTarget {
        val bmr = formula.basalMetabolicRate(
            BodyMetrics(profile.sex, profile.ageYears, profile.weightKg, profile.heightCm),
        )
        val tdee = bmr * profile.activityLevel.multiplier
        val adjustment = profile.weeklyRateKg
            ?.let { WeightGoalPlanner.dailyAdjustmentKcal(profile.goal, it) }
            ?: goalAdjustmentKcal(profile.goal)
        val raw = (tdee + adjustment).roundToInt()
        val minimum = minimumKcal(profile.sex)
        val suggested = maxOf(raw, minimum)
        val manual = profile.manualCalorieTarget
        val effective = manual ?: suggested
        return CalorieTarget(
            bmrKcal = bmr,
            tdeeKcal = tdee,
            suggestedKcal = suggested,
            effectiveKcal = effective,
            isManual = manual != null,
            minimumApplied = raw < minimum,
            manualBelowMinimum = manual != null && manual < minimum,
            macros = macroTargets(effective, profile.goal),
        )
    }

    companion object {
        const val KCAL_PER_G_PROTEIN = 4.0
        const val KCAL_PER_G_CARBS = 4.0
        const val KCAL_PER_G_FAT = 9.0

        /** Ajustes moderados y habituales sobre el gasto estimado. */
        fun goalAdjustmentKcal(goal: Goal): Int = when (goal) {
            Goal.LOSE_WEIGHT -> -500
            Goal.MAINTAIN_WEIGHT -> 0
            Goal.GAIN_WEIGHT -> 300
            Goal.GAIN_MUSCLE -> 250
        }

        /** Mínimos orientativos habituales; por debajo, la app no sugiere objetivo y avisa. */
        fun minimumKcal(sex: Sex): Int = when (sex) {
            Sex.MALE -> 1500
            Sex.FEMALE -> 1200
        }

        fun macroTargets(kcal: Int, goal: Goal): MacroTargets {
            val proteinShare = when (goal) {
                Goal.LOSE_WEIGHT, Goal.GAIN_MUSCLE -> 0.30
                Goal.MAINTAIN_WEIGHT -> 0.25
                Goal.GAIN_WEIGHT -> 0.20
            }
            val fatShare = 0.30
            val carbShare = 1.0 - proteinShare - fatShare
            return MacroTargets(
                proteinG = kcal * proteinShare / KCAL_PER_G_PROTEIN,
                carbohydratesG = kcal * carbShare / KCAL_PER_G_CARBS,
                fatG = kcal * fatShare / KCAL_PER_G_FAT,
            )
        }
    }
}
