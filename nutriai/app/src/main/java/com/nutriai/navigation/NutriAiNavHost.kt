package com.nutriai.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nutriai.feature.home.PhaseOneScreen
import kotlinx.serialization.Serializable

/** Rutas tipadas. Cada fase añadirá aquí sus pantallas. */
@Serializable
data object PhaseOneRoute

@Composable
fun NutriAiNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = PhaseOneRoute) {
        composable<PhaseOneRoute> { PhaseOneScreen() }
    }
}
