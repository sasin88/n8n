package com.nutriai.domain.nutrition

import com.nutriai.domain.model.DailySummary
import com.nutriai.domain.model.Meal
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.model.WeightRecord
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object NutritionStats {

    /** Un resumen por día del rango, incluidos los días sin comidas (con ceros). */
    fun dailySummaries(meals: List<Meal>, from: LocalDate, to: LocalDate): List<DailySummary> {
        val byDate = meals.groupBy { it.date }
        return generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .map { date ->
                val dayMeals = byDate[date].orEmpty()
                DailySummary(date, Nutrients.sum(dayMeals.map { it.nutrients }), dayMeals.size)
            }
            .toList()
    }

    /** Promedio solo sobre los días con al menos una comida registrada. */
    fun averageOfLoggedDays(summaries: List<DailySummary>): Nutrients? {
        val logged = summaries.filter { it.mealCount > 0 }
        if (logged.isEmpty()) return null
        return Nutrients.sum(logged.map { it.nutrients }).scaledBy(1.0 / logged.size)
    }

    /**
     * Tendencia de peso en kg por semana (regresión lineal). null si no hay datos suficientes.
     */
    fun weightTrendKgPerWeek(records: List<WeightRecord>): Double? {
        if (records.size < 2) return null
        val origin = records.minOf { it.date }
        val xs = records.map { ChronoUnit.DAYS.between(origin, it.date).toDouble() }
        val ys = records.map { it.weightKg }
        val meanX = xs.average()
        val meanY = ys.average()
        val denominator = xs.sumOf { (it - meanX) * (it - meanX) }
        if (denominator == 0.0) return null
        val slopePerDay = xs.indices.sumOf { (xs[it] - meanX) * (ys[it] - meanY) } / denominator
        return slopePerDay * 7
    }
}
