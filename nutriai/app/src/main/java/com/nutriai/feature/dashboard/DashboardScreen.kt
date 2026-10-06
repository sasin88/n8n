package com.nutriai.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutriai.core.ui.components.AppearIn
import com.nutriai.core.ui.components.CalorieRing
import com.nutriai.core.ui.components.EstimateNote
import com.nutriai.core.ui.components.GlassIconButton
import com.nutriai.core.ui.components.LargeTitle
import com.nutriai.core.ui.components.MacroBar
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.core.ui.components.glass
import com.nutriai.core.ui.format.Labels
import com.nutriai.core.ui.theme.CarbsColor
import com.nutriai.core.ui.theme.ExerciseColor
import com.nutriai.core.ui.theme.FastingColor
import com.nutriai.core.ui.theme.FatColor
import com.nutriai.core.ui.theme.ProteinColor
import com.nutriai.core.ui.theme.WaterColor
import com.nutriai.core.ui.theme.WeightColor
import com.nutriai.domain.activity.ExerciseType
import com.nutriai.domain.health.Hydration
import com.nutriai.domain.model.MealType
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.model.Units
import com.nutriai.domain.model.UnitSystem
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    onAnalyze: () -> Unit,
    onRegisterMeal: (MealType?) -> Unit,
    onOpenDay: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenWeight: () -> Unit,
    onOpenFasting: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshDay() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var addingExercise by remember { mutableStateOf(false) }
    DashboardContent(
        state = state,
        onAnalyze = onAnalyze,
        onRegisterMeal = onRegisterMeal,
        onOpenDay = onOpenDay,
        onOpenProfile = onOpenProfile,
        onAddWater = { viewModel.addWater() },
        onRemoveWater = { viewModel.removeWater() },
        onAddExercise = { addingExercise = true },
        onOpenWeight = onOpenWeight,
        onOpenFasting = onOpenFasting,
    )
    if (addingExercise) {
        ExerciseDialog(
            weightKg = state.data?.profile?.weightKg ?: 70.0,
            onDismiss = { addingExercise = false },
            onSave = { type, minutes, kcal -> viewModel.addExercise(type, minutes, kcal); addingExercise = false },
        )
    }
}

@Composable
fun DashboardContent(
    state: DashboardState,
    onAnalyze: () -> Unit,
    onRegisterMeal: (MealType?) -> Unit,
    onOpenDay: () -> Unit,
    onOpenProfile: () -> Unit,
    onAddWater: () -> Unit = {},
    onRemoveWater: () -> Unit = {},
    onAddExercise: () -> Unit = {},
    onOpenWeight: () -> Unit = {},
    onOpenFasting: () -> Unit = {},
) {
    val data = state.data
    if (state.loading || data == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val target = data.target
    val budget = state.budgetKcal
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            LargeTitle(title = "Hoy", subtitle = "${Labels.greeting()}, ${data.profile.name}") {
                GlassIconButton(onClick = onOpenProfile, contentDescription = "Perfil") {
                    Text(data.profile.name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            AppearIn(0) {
                NutriCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CalorieRing(consumed = state.consumed.energyKcal, target = budget, size = 150.dp)
                        Spacer(Modifier.width(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Stat("Presupuesto", Labels.kcal(budget.toDouble()))
                            Stat("Consumido", Labels.approxKcal(state.consumed.energyKcal), Modifier.testTag("consumed"))
                            Stat("Ejercicio", "+${Labels.kcal(state.burnedKcal.toDouble())}", color = ExerciseColor)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    MacroBar("Proteínas", state.consumed.proteinG, target.macros.proteinG, ProteinColor)
                    Spacer(Modifier.height(10.dp))
                    MacroBar("Carbohidratos", state.consumed.carbohydratesG, target.macros.carbohydratesG, CarbsColor)
                    Spacer(Modifier.height(10.dp))
                    MacroBar("Grasas", state.consumed.fatG, target.macros.fatG, FatColor)
                    Spacer(Modifier.height(12.dp))
                    EstimateNote()
                }
            }
        }
        item {
            AppearIn(1) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(30.dp))
                            .then(Modifier.glass(RoundedCornerShape(30.dp), onClick = onAnalyze))
                            .testTag("analyze"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Analizar comida", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(
                        Modifier.weight(1f).height(60.dp).glass(RoundedCornerShape(30.dp), onClick = { onRegisterMeal(null) }).testTag("register"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Registrar", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        MealType.entries.forEachIndexed { index, type ->
            item(key = type.name) {
                AppearIn(2 + index) {
                    MealTypeCard(
                        type = type,
                        nutrients = state.meals[type]?.let { meals -> Nutrients.sum(meals.map { it.nutrients }) },
                        itemsSummary = state.meals[type].orEmpty().flatMap { it.items }.joinToString(", ") { it.name },
                        onAdd = { onRegisterMeal(type) },
                        onOpen = onOpenDay,
                    )
                }
            }
        }
        item { AppearIn(6) { WaterCard(state.waterMl, state.waterGoalMl, onAddWater, onRemoveWater) } }
        item {
            AppearIn(7) {
                NutriCard(onClick = onAddExercise) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Ejercicio", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (state.exercise.isEmpty()) "Aún no registras ejercicio hoy"
                                else state.exercise.joinToString(" · ") { "${Labels.exerciseEmoji(it.type)} ${it.minutes} min" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        Text("${state.burnedKcal} kcal", style = MaterialTheme.typography.titleMedium, color = ExerciseColor)
                    }
                }
            }
        }
        item {
            AppearIn(8) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val p = data.profile
                    val imperial = p.unitSystem == UnitSystem.IMPERIAL
                    fun w(kg: Double) = if (imperial) "${Units.kgToLb(kg).roundToInt()} lb" else Labels.weight(kg)
                    NutriCard(Modifier.weight(1f), onClick = onOpenWeight, contentPadding = PaddingValues(16.dp)) {
                        Text("⚖️ Peso", style = MaterialTheme.typography.labelMedium, color = WeightColor)
                        Spacer(Modifier.height(6.dp))
                        Text(w(p.weightKg), style = MaterialTheme.typography.titleLarge)
                        p.targetWeightKg?.let { Text("Meta ${w(it)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    NutriCard(Modifier.weight(1f), onClick = onOpenFasting, contentPadding = PaddingValues(16.dp)) {
                        Text("⏱️ Ayuno", style = MaterialTheme.typography.labelMedium, color = FastingColor)
                        Spacer(Modifier.height(6.dp))
                        val fast = state.activeFast
                        if (fast == null) {
                            Text("Sin ayuno", style = MaterialTheme.typography.titleLarge)
                            Text("Toca para empezar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            val now = System.currentTimeMillis()
                            Text(Labels.duration(fast.elapsedMs(now)), style = MaterialTheme.typography.titleLarge)
                            Text("Plan ${fast.protocol.label}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color)
    }
}

@Composable
private fun WaterCard(ml: Int, goalMl: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
    NutriCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💧", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Agua", style = MaterialTheme.typography.titleMedium)
                Text("${Labels.formatNumber(ml / 1000.0)} de ${Labels.formatNumber(goalMl / 1000.0)} L · 1 taza = ${Hydration.CUP_ML} ml", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRemove, enabled = ml > 0) { Icon(Icons.Rounded.Remove, "Quitar una taza") }
            GlassIconButton(onClick = onAdd, contentDescription = "Añadir una taza", modifier = Modifier.testTag("add_water")) {
                Icon(Icons.Rounded.Add, null, tint = WaterColor)
            }
        }
        Spacer(Modifier.height(12.dp))
        val cups = (goalMl + Hydration.CUP_ML - 1) / Hydration.CUP_ML
        val filled = ml / Hydration.CUP_ML
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(cups.coerceAtMost(16)) { i ->
                val shape = RoundedCornerShape(8.dp)
                Box(
                    if (i < filled) Modifier.weight(1f).height(26.dp).clip(shape).background(WaterColor.copy(alpha = 0.85f))
                    else Modifier.weight(1f).height(26.dp).glass(shape, strength = 0.6f),
                )
            }
        }
    }
}
