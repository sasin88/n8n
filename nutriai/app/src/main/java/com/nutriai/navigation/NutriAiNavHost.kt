package com.nutriai.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nutriai.feature.analyze.AnalyzeScreen
import com.nutriai.feature.analyze.CameraScreen
import com.nutriai.feature.dashboard.DashboardScreen
import com.nutriai.feature.history.DayDetailScreen
import com.nutriai.feature.history.HistoryScreen
import com.nutriai.feature.history.StatsScreen
import com.nutriai.feature.meal.MealEditorScreen
import com.nutriai.feature.meal.SavedScreen
import com.nutriai.feature.onboarding.OnboardingScreen
import com.nutriai.feature.profile.ProfileFormScreen
import com.nutriai.feature.profile.ProfilePickerScreen
import com.nutriai.feature.profile.ProfileScreen
import com.nutriai.feature.settings.PrivacyScreen
import com.nutriai.feature.settings.SettingsScreen
import kotlin.reflect.KClass

private data class Tab(val route: Any, val routeClass: KClass<*>, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(DashboardRoute, DashboardRoute::class, "Hoy", Icons.Default.Home),
    Tab(HistoryRoute, HistoryRoute::class, "Historial", Icons.Default.DateRange),
    Tab(WeightRoute, WeightRoute::class, "Peso", Icons.Default.Favorite),
    Tab(ProfileRoute, ProfileRoute::class, "Perfil", Icons.Default.Person),
)

@Composable
fun NutriAiNavHost(hasProfile: Boolean) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    // El destino inicial se fija una vez; los cambios posteriores se gestionan navegando.
    val startDestination: Any = remember { if (hasProfile) DashboardRoute else OnboardingRoute }
    val showBottomBar = tabs.any { tab -> destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true }

    // Si se elimina el último perfil, volver a la bienvenida.
    LaunchedEffect(hasProfile) {
        if (!hasProfile && destination != null && destination.hasRoute(OnboardingRoute::class).not() &&
            destination.hasRoute(ProfileFormRoute::class).not()
        ) {
            navController.navigate(OnboardingRoute) { popUpTo(0) { inclusive = true } }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true,
                            onClick = { navController.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 } },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(240)) { it / 12 } },
        ) {
            composable<OnboardingRoute> {
                OnboardingScreen(onStart = { navController.navigate(ProfileFormRoute()) })
            }
            composable<ProfileFormRoute> { entry ->
                val editing = entry.toRoute<ProfileFormRoute>().profileId >= 0
                ProfileFormScreen(
                    onSaved = {
                        if (editing) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(DashboardRoute) { popUpTo(0) { inclusive = true } }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<ProfilePickerRoute> {
                ProfilePickerScreen(onBack = { navController.popBackStack() }, onCreateNew = { navController.navigate(ProfileFormRoute()) })
            }
            composable<DashboardRoute> {
                DashboardScreen(
                    onAnalyze = { navController.navigate(AnalyzeRoute()) },
                    onRegisterMeal = { type -> navController.navigate(MealEditorRoute(mealType = type?.name.orEmpty())) },
                    onOpenDay = { navController.switchTab(HistoryRoute) },
                    onSwitchProfile = { navController.navigate(ProfilePickerRoute) },
                )
            }
            composable<HistoryRoute> {
                HistoryScreen(
                    onOpenDay = { navController.navigate(DayDetailRoute(it.toEpochDay())) },
                    onOpenMeal = { navController.navigate(MealEditorRoute(mealId = it)) },
                    onOpenStats = { navController.navigate(StatsRoute) },
                )
            }
            composable<WeightRoute> { com.nutriai.feature.weight.WeightScreen() }
            composable<ProfileRoute> {
                ProfileScreen(
                    onEdit = { navController.navigate(ProfileFormRoute(it)) },
                    onSwitch = { navController.navigate(ProfilePickerRoute) },
                    onSettings = { navController.navigate(SettingsRoute) },
                    onPrivacy = { navController.navigate(PrivacyRoute) },
                )
            }
            composable<AnalyzeRoute> { entry ->
                val mealType = entry.toRoute<AnalyzeRoute>().mealType
                AnalyzeScreen(
                    onBack = { navController.popBackStack() },
                    onCamera = { navController.navigate(CameraRoute(mealType)) },
                    onImagePicked = { uri -> navController.navigate(MealEditorRoute(mealType = mealType, imageUri = uri.toString())) },
                    onManual = { navController.navigate(MealEditorRoute(mealType = mealType)) },
                )
            }
            composable<CameraRoute> { entry ->
                val mealType = entry.toRoute<CameraRoute>().mealType
                CameraScreen(
                    onPhotoTaken = { uri ->
                        navController.navigate(MealEditorRoute(mealType = mealType, imageUri = uri.toString(), deleteImageAfter = true)) {
                            popUpTo<CameraRoute> { inclusive = true }
                        }
                    },
                    onClose = { navController.popBackStack() },
                    onUseGallery = { navController.popBackStack() },
                )
            }
            composable<MealEditorRoute> {
                MealEditorScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { kcal ->
                        navController.navigate(SavedRoute(kcal)) { popUpTo(DashboardRoute) { inclusive = false } }
                    },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable<SavedRoute> { entry ->
                SavedScreen(kcal = entry.toRoute<SavedRoute>().kcal, onDone = { navController.popBackStack(DashboardRoute, inclusive = false) })
            }
            composable<DayDetailRoute> {
                DayDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenMeal = { navController.navigate(MealEditorRoute(mealId = it)) },
                    onAddMeal = { date -> navController.navigate(MealEditorRoute(epochDay = date.toEpochDay())) },
                )
            }
            composable<StatsRoute> { StatsScreen(onBack = { navController.popBackStack() }) }
            composable<SettingsRoute> { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable<PrivacyRoute> { PrivacyScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

private fun NavHostController.switchTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
