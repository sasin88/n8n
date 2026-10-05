package com.nutriai.domain.model

/** Perfil local. Todos los datos personales (comidas, peso, estadísticas) cuelgan de [id]. */
data class UserProfile(
    val id: Long = 0,
    val name: String,
    val sex: Sex,
    val ageYears: Int,
    val weightKg: Double,
    val heightCm: Double,
    val activityLevel: ActivityLevel,
    val goal: Goal,
    val targetWeightKg: Double? = null,
    /** Objetivo calórico fijado a mano por el usuario; si es null se usa el calculado. */
    val manualCalorieTarget: Int? = null,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val countryCode: String? = null,
    val languageTag: String? = null,
)

enum class Sex { MALE, FEMALE }

/**
 * Factores de actividad habituales para estimar el TDEE a partir del BMR.
 * Son aproximaciones poblacionales, no una medición individual.
 */
enum class ActivityLevel(val multiplier: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725),
    VERY_ACTIVE(1.9),
}

enum class Goal { LOSE_WEIGHT, MAINTAIN_WEIGHT, GAIN_WEIGHT, GAIN_MUSCLE }

enum class UnitSystem { METRIC, IMPERIAL }

/** Preferencia de tema: seguir al sistema o forzar claro/oscuro. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }
