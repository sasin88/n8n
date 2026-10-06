package com.nutriai.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nutriai.core.ui.components.GlassBackground
import com.nutriai.core.ui.components.glass
import com.nutriai.feature.analyze.AnalyzeScreen
import com.nutriai.feature.analyze.CameraScreen
import com.nutriai.feature.dashboard.DashboardScreen
import com.nutriai.feature.fasting.FastingScreen
import com.nutriai.feature.history.DayDetailScreen
import com.nutriai.feature.history.HistoryScreen
import com.nutriai.feature.history.StatsScreen
import com.nutriai.feature.meal.MealEditorScreen
import com.nutriai.feature.meal.SavedScreen
import com.nutriai.feature.onboarding.OnboardingFlowScreen
import com.nutriai.feature.profile.ProfileFormScreen
import com.nutriai.feature.profile.ProfilePickerScreen
import com.nutriai.feature.profile.ProfileScreen
import com.nutriai.feature.progress.ProgressScreen
import com.nutriai.feature.recipes.CreateRecipeScreen
import com.nutriai.feature.recipes.RecipeDetailScreen
import com.nutriai.feature.recipes.RecipesScreen
import com.nutriai.feature.settings.GoalScreen
import com.nutriai.feature.settings.NotificationsScreen
import com.nutriai.feature.settings.PrivacyScreen
import com.nutriai.feature.settings.SettingsScreen
import com.nutriai.feature.weight.WeightScreen
import kotlin.reflect.KClass

private data class Tab(val route: Any, val routeClass: KClass<*>, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(DashboardRoute, DashboardRoute::class, "Hoy", Icons.Rounded.Home),
    Tab(RecipesRoute, RecipesRoute::class, "Recetas", Icons.Rounded.RestaurantMenu),
    Tab(FastingRoute, FastingRoute::class, "Ayuno", Icons.Rounded.Timer),
    Tab(ProgressRoute, ProgressRoute::class, "Progreso", Icons.Rounded.Insights),
)

@Composable
fun NutriAiNavHost(hasProfile: Boolean) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    // El destino inicial se fija una vez; los cambios posteriores se gestionan navegando.
    val startDestination: Any = remember { if (hasProfile) DashboardRoute else OnboardingRoute }
    val showTabBar = tabs.any { tab -> destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true }
    var addMenu by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(hasProfile) {
        if (!hasProfile && destination != null && !destination.hasRoute(OnboardingRoute::class)) {
            navController.navigate(OnboardingRoute) { popUpTo(0) { inclusive = true } }
        }
    }

    GlassBackground {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(280)) { it / 10 } },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(240)) { it / 10 } },
        ) {
            composable<OnboardingRoute> {
                OnboardingFlowScreen(onDone = { navController.navigate(DashboardRoute) { popUpTo(0) { inclusive = true } } })
            }
            composable<ProfileFormRoute> { entry ->
                val editing = entry.toRoute<ProfileFormRoute>().profileId >= 0
                if (editing) {
                    ProfileFormScreen(onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
                } else {
                    // Crear otro perfil usa el mismo asistente de bienvenida.
                    OnboardingFlowScreen(onDone = { navController.navigate(DashboardRoute) { popUpTo(0) { inclusive = true } } })
                }
            }
            composable<ProfilePickerRoute> {
                ProfilePickerScreen(onBack = { navController.popBackStack() }, onCreateNew = { navController.navigate(ProfileFormRoute()) })
            }
            composable<DashboardRoute> {
                DashboardScreen(
                    onAnalyze = { navController.navigate(AnalyzeRoute()) },
                    onRegisterMeal = { type -> navController.navigate(MealEditorRoute(mealType = type?.name.orEmpty())) },
                    onOpenDay = { navController.navigate(DayDetailRoute(java.time.LocalDate.now().toEpochDay())) },
                    onOpenProfile = { navController.navigate(ProfileRoute) },
                    onOpenWeight = { navController.navigate(WeightRoute) },
                    onOpenFasting = { navController.switchTab(FastingRoute) },
                )
            }
            composable<RecipesRoute> {
                RecipesScreen(
                    onOpenRecipe = { navController.navigate(RecipeDetailRoute(it)) },
                    onCreateRecipe = { navController.navigate(CreateRecipeRoute) },
                )
            }
            composable<RecipeDetailRoute> {
                RecipeDetailScreen(onBack = { navController.popBackStack() }, onLog = { id -> navController.navigate(MealEditorRoute(foodId = id)) })
            }
            composable<CreateRecipeRoute> {
                CreateRecipeScreen(onBack = { navController.popBackStack() }, onSaved = { id ->
                    navController.navigate(RecipeDetailRoute(id)) { popUpTo<CreateRecipeRoute> { inclusive = true } }
                })
            }
            composable<FastingRoute> { FastingScreen() }
            composable<ProgressRoute> {
                ProgressScreen(
                    onOpenWeight = { navController.navigate(WeightRoute) },
                    onOpenHistory = { navController.navigate(HistoryRoute) },
                    onOpenStats = { navController.navigate(StatsRoute) },
                )
            }
            composable<HistoryRoute> {
                HistoryScreen(
                    onOpenDay = { navController.navigate(DayDetailRoute(it.toEpochDay())) },
                    onOpenMeal = { navController.navigate(MealEditorRoute(mealId = it)) },
                    onOpenStats = { navController.navigate(StatsRoute) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<WeightRoute> { WeightScreen(onBack = { navController.popBackStack() }) }
            composable<ProfileRoute> {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(ProfileFormRoute(it)) },
                    onSwitch = { navController.navigate(ProfilePickerRoute) },
                    onGoal = { navController.navigate(GoalRoute) },
                    onPreferences = { navController.navigate(SettingsRoute) },
                    onNotifications = { navController.navigate(NotificationsRoute) },
                    onPrivacy = { navController.navigate(PrivacyRoute) },
                )
            }
            composable<GoalRoute> { GoalScreen(onBack = { navController.popBackStack() }) }
            composable<NotificationsRoute> { NotificationsScreen(onBack = { navController.popBackStack() }) }
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
                    onSaved = { kcal -> navController.navigate(SavedRoute(kcal)) { popUpTo(DashboardRoute) { inclusive = false } } },
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

        // Menú del botón "+": acciones rápidas sobre un velo translúcido.
        AnimatedVisibility(addMenu, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)).clickable { addMenu = false })
        }
        AnimatedVisibility(
            visible = addMenu,
            enter = slideInVertically { it / 3 } + fadeIn(),
            exit = slideOutVertically { it / 3 } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 110.dp).fillMaxWidth()
                    .glass(RoundedCornerShape(30.dp)).padding(8.dp),
            ) {
                QuickAction("📷", "Analizar comida con IA") { addMenu = false; navController.navigate(AnalyzeRoute()) }
                QuickAction("✍️", "Registrar comida") { addMenu = false; navController.navigate(MealEditorRoute()) }
                QuickAction("🍲", "Crear receta") { addMenu = false; navController.navigate(CreateRecipeRoute) }
                QuickAction("⚖️", "Registrar peso") { addMenu = false; navController.navigate(WeightRoute) }
                QuickAction("⏱️", "Ayuno") { addMenu = false; navController.switchTab(FastingRoute) }
            }
        }

        AnimatedVisibility(
            visible = showTabBar,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            GlassTabBar(
                isSelected = { tab -> destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true },
                onSelect = { addMenu = false; navController.switchTab(it.route) },
                addOpen = addMenu,
                onAdd = { addMenu = !addMenu },
            )
        }
    }
}

@Composable
private fun GlassTabBar(isSelected: (Tab) -> Boolean, onSelect: (Tab) -> Unit, addOpen: Boolean, onAdd: () -> Unit) {
    Row(
        Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.weight(1f).height(66.dp).glass(RoundedCornerShape(33.dp)).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = isSelected(tab)
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(26.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { onSelect(tab) }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Icon(tab.icon, contentDescription = null, tint = color)
                    Text(tab.label, style = MaterialTheme.typography.labelSmall, color = color)
                }
            }
        }
        Box(
            Modifier.size(66.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable(onClick = onAdd).testTag("add_menu"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Add, contentDescription = if (addOpen) "Cerrar menú" else "Añadir",
                tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun QuickAction(emoji: String, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun NavHostController.switchTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
