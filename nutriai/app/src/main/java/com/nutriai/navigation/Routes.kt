package com.nutriai.navigation

import kotlinx.serialization.Serializable

/** Rutas tipadas de la app. Los argumentos opcionales usan valores centinela (-1 / ""). */
@Serializable data object OnboardingRoute
@Serializable data class ProfileFormRoute(val profileId: Long = -1)
@Serializable data object ProfilePickerRoute

// Pestañas
@Serializable data object DashboardRoute
@Serializable data object RecipesRoute
@Serializable data object FastingRoute
@Serializable data object ProgressRoute

@Serializable data object HistoryRoute
@Serializable data object WeightRoute
@Serializable data object ProfileRoute
@Serializable data object GoalRoute
@Serializable data object NotificationsRoute
@Serializable data class RecipeDetailRoute(val recipeId: String)
@Serializable data object CreateRecipeRoute

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
    /** Alimento o receta con el que empezar (p. ej. "Registrar" desde una receta). */
    val foodId: String = "",
)

@Serializable data class SavedRoute(val kcal: Int)
@Serializable data class DayDetailRoute(val epochDay: Long)
@Serializable data object StatsRoute
@Serializable data object SettingsRoute
@Serializable data object PrivacyRoute
