package com.nutriai.domain.model

import kotlin.math.roundToInt

/** Conversión entre unidades métricas e imperiales. Internamente todo se guarda en kg y cm. */
object Units {
    const val LB_PER_KG = 2.2046226218
    const val CM_PER_INCH = 2.54

    fun kgToLb(kg: Double) = kg * LB_PER_KG
    fun lbToKg(lb: Double) = lb / LB_PER_KG

    /** (pies, pulgadas) */
    fun cmToFeetInches(cm: Double): Pair<Int, Int> {
        val totalInches = (cm / CM_PER_INCH).roundToInt()
        return totalInches / 12 to totalInches % 12
    }

    fun feetInchesToCm(feet: Int, inches: Int) = (feet * 12 + inches) * CM_PER_INCH
}
