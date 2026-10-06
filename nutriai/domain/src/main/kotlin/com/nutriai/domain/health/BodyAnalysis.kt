package com.nutriai.domain.health

import kotlin.math.pow

/** Categorías de IMC de la OMS para adultos. */
enum class BmiCategory { UNDERWEIGHT, HEALTHY, OVERWEIGHT, OBESITY }

data class BmiResult(val bmi: Double, val category: BmiCategory, val healthyWeightKg: ClosedFloatingPointRange<Double>)

/**
 * Índice de masa corporal y rango de peso saludable (IMC 18.5–24.9, criterio OMS para adultos).
 * Es un indicador poblacional: no distingue músculo de grasa y no es un diagnóstico.
 */
object BodyAnalysis {
    const val HEALTHY_MIN = 18.5
    const val HEALTHY_MAX = 24.9
    const val OBESITY_MIN = 30.0

    fun bmi(weightKg: Double, heightCm: Double): Double {
        require(weightKg > 0 && heightCm > 0) { "Peso y altura deben ser positivos" }
        return weightKg / (heightCm / 100.0).pow(2)
    }

    fun category(bmi: Double): BmiCategory = when {
        bmi < HEALTHY_MIN -> BmiCategory.UNDERWEIGHT
        bmi < 25.0 -> BmiCategory.HEALTHY
        bmi < OBESITY_MIN -> BmiCategory.OVERWEIGHT
        else -> BmiCategory.OBESITY
    }

    fun healthyWeightRange(heightCm: Double): ClosedFloatingPointRange<Double> {
        val m2 = (heightCm / 100.0).pow(2)
        return (HEALTHY_MIN * m2)..(HEALTHY_MAX * m2)
    }

    fun analyze(weightKg: Double, heightCm: Double): BmiResult {
        val value = bmi(weightKg, heightCm)
        return BmiResult(value, category(value), healthyWeightRange(heightCm))
    }
}
