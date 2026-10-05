package com.nutriai.data.repository

import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Abstracción del reloj para poder probar fechas de forma determinista. */
interface Clock {
    fun nowEpochMs(): Long
    fun today(): LocalDate
}

class SystemClock @Inject constructor() : Clock {
    override fun nowEpochMs(): Long = System.currentTimeMillis()
    override fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())
}
