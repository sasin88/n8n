package com.nutriai.domain.activity

import kotlin.math.roundToInt

/**
 * Actividades con su equivalente metabólico (MET) aproximado, según los valores de referencia del
 * Compendium of Physical Activities. Las calorías resultantes son estimaciones.
 */
enum class ExerciseType(val met: Double) {
    WALKING(3.5),
    BRISK_WALKING(4.3),
    RUNNING_8KMH(8.3),
    RUNNING_10KMH(9.8),
    CYCLING_MODERATE(6.8),
    SWIMMING_MODERATE(5.8),
    STRENGTH_TRAINING(3.5),
    YOGA(2.5),
    DANCING(5.0),
    SOCCER(7.0),
    OTHER(0.0),
}

object ExerciseCalculator {
    /** kcal ≈ MET × peso (kg) × horas. */
    fun estimateKcal(type: ExerciseType, minutes: Int, weightKg: Double): Int? {
        if (type == ExerciseType.OTHER || minutes <= 0 || weightKg <= 0) return null
        return (type.met * weightKg * minutes / 60.0).roundToInt()
    }

    /** Minutos de una actividad equivalentes a cierta cantidad de kcal (para comparaciones). */
    fun minutesFor(kcal: Int, type: ExerciseType, weightKg: Double): Int? {
        if (type.met <= 0 || weightKg <= 0 || kcal <= 0) return null
        return (kcal / (type.met * weightKg) * 60).roundToInt()
    }
}
