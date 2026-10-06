package com.nutriai.domain.model

import com.nutriai.domain.activity.ExerciseType
import java.time.LocalDate

data class WaterLog(val id: Long = 0, val profileId: Long, val date: LocalDate, val ml: Int, val createdAtEpochMs: Long)

data class ExerciseLog(
    val id: Long = 0,
    val profileId: Long,
    val date: LocalDate,
    val type: ExerciseType,
    val minutes: Int,
    val kcal: Int,
    /** true si las kcal las calculó la app (estimación MET); false si las introdujo el usuario. */
    val estimated: Boolean,
    val note: String? = null,
)
