package com.nutriai.navigation

import kotlinx.serialization.Serializable

/** Rutas tipadas de la app. Los argumentos opcionales usan valores centinela (-1 / ""). */
@Serializable data object OnboardingRoute
@Serializable data class ProfileFormRoute(val profileId: Long = -1)
@Serializable data object ProfilePickerRoute

@Serializable data object DashboardRoute
@Serializable data object HistoryRoute
@Serializable data object WeightRoute
@Serializable data object ProfileRoute

@Serializable data class AnalyzeRoute(val mealType: String = "")
@Serializable data class CameraRoute(val mealType: String = "")

/**
 * Editor de comidas. Sirve para: registro manual (sin argumentos), edición (mealId) y
 * resultado de un análisis de IA (imageUri).
 */
@Serializable
data class MealEditorRoute(
    val mealId: Long = -1,
    val mealType: String = "",
    val epochDay: Long = Long.MIN_VALUE,
    val imageUri: String = "",
    val deleteImageAfter: Boolean = false,
)

@Serializable data class SavedRoute(val kcal: Int)
@Serializable data class DayDetailRoute(val epochDay: Long)
@Serializable data object StatsRoute
@Serializable data object SettingsRoute
@Serializable data object PrivacyRoute
