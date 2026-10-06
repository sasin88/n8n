package com.nutriai.domain.health

import kotlin.math.roundToInt

/** Meta de agua orientativa: ~35 ml por kg de peso, redondeada a 250 ml y limitada a un rango prudente. */
object Hydration {
    const val CUP_ML = 250

    fun suggestedGoalMl(weightKg: Double): Int {
        val raw = weightKg * 35
        val rounded = (raw / CUP_ML).roundToInt() * CUP_ML
        return rounded.coerceIn(1500, 4000)
    }
}
