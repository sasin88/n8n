package com.nutriai.domain.health

import com.nutriai.domain.model.Goal
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil

enum class GoalDifficulty { EASY, MODERATE, CHALLENGING }

/** Ritmos de cambio ofrecidos. 0.5 kg/semana es el ritmo recomendado habitualmente. */
enum class WeightPace(val kgPerWeek: Double) { SLOW(0.25), RECOMMENDED(0.5), FAST(0.75) }

data class WeightPlan(
    val targetWeightKg: Double,
    val kgPerWeek: Double,
    /** Ajuste diario sobre el gasto estimado (negativo = déficit). */
    val dailyAdjustmentKcal: Int,
    val weeks: Int,
    val estimatedDate: LocalDate,
    val changePercent: Double,
    val difficulty: GoalDifficulty,
)

/** Validación del peso objetivo: nunca se acepta un objetivo con IMC por debajo de lo saludable. */
sealed interface TargetValidation {
    data object Ok : TargetValidation
    data class BelowHealthy(val minimumKg: Double) : TargetValidation
    data object WrongDirection : TargetValidation
}

/**
 * Planificación orientativa de un objetivo de peso. Usa la aproximación habitual de
 * ~7700 kcal por kg de peso corporal; el cambio real varía por persona.
 */
object WeightGoalPlanner {
    const val KCAL_PER_KG = 7700.0

    fun validateTarget(currentKg: Double, targetKg: Double, heightCm: Double, goal: Goal): TargetValidation {
        val minimum = BodyAnalysis.healthyWeightRange(heightCm).start
        if (targetKg < minimum && targetKg < currentKg) return TargetValidation.BelowHealthy(minimum)
        return when (goal) {
            Goal.LOSE_WEIGHT -> if (targetKg >= currentKg) TargetValidation.WrongDirection else TargetValidation.Ok
            Goal.GAIN_WEIGHT, Goal.GAIN_MUSCLE -> if (targetKg <= currentKg) TargetValidation.WrongDirection else TargetValidation.Ok
            Goal.MAINTAIN_WEIGHT -> TargetValidation.Ok
        }
    }

    fun dailyAdjustmentKcal(goal: Goal, kgPerWeek: Double): Int = when (goal) {
        Goal.MAINTAIN_WEIGHT -> 0
        Goal.LOSE_WEIGHT -> -(kgPerWeek * KCAL_PER_KG / 7).toInt()
        // Para ganar se usa un superávit más prudente que el déficit equivalente.
        Goal.GAIN_WEIGHT -> (kgPerWeek * KCAL_PER_KG / 7 * 0.6).toInt()
        Goal.GAIN_MUSCLE -> (kgPerWeek * KCAL_PER_KG / 7 * 0.5).toInt()
    }

    fun plan(currentKg: Double, targetKg: Double, goal: Goal, pace: Double, today: LocalDate): WeightPlan {
        val diff = abs(targetKg - currentKg)
        val weeks = if (goal == Goal.MAINTAIN_WEIGHT || diff < 0.1) 0 else ceil(diff / pace).toInt()
        val percent = if (currentKg > 0) diff / currentKg * 100 else 0.0
        val difficulty = when {
            percent <= 10 -> GoalDifficulty.EASY
            percent <= 20 -> GoalDifficulty.MODERATE
            else -> GoalDifficulty.CHALLENGING
        }
        return WeightPlan(
            targetWeightKg = targetKg,
            kgPerWeek = pace,
            dailyAdjustmentKcal = dailyAdjustmentKcal(goal, pace),
            weeks = weeks,
            estimatedDate = today.plusWeeks(weeks.toLong()),
            changePercent = percent,
            difficulty = difficulty,
        )
    }

    /** Curva prevista: lenta al principio, como suele ocurrir; termina en el objetivo. */
    fun projection(currentKg: Double, targetKg: Double, weeks: Int, points: Int = 8): List<Double> {
        if (weeks <= 0) return listOf(currentKg, targetKg)
        return (0 until points).map { i ->
            val t = i.toDouble() / (points - 1)
            val eased = t * t * (3 - 2 * t)
            currentKg + (targetKg - currentKg) * eased
        }
    }
}
