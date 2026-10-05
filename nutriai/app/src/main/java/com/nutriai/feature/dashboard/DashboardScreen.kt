package com.nutriai.feature.dashboard

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutriai.core.ui.components.AppearIn
import com.nutriai.core.ui.components.CalorieRing
import com.nutriai.core.ui.components.EstimateNote
import com.nutriai.core.ui.components.MacroBar
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.core.ui.format.Labels
import com.nutriai.core.ui.theme.CarbsColor
import com.nutriai.core.ui.theme.FatColor
import com.nutriai.core.ui.theme.ProteinColor
import com.nutriai.domain.model.MealType
import com.nutriai.domain.model.Nutrients

@Composable
fun DashboardScreen(
    onAnalyze: () -> Unit,
    onRegisterMeal: (MealType?) -> Unit,
    onOpenDay: () -> Unit,
    onSwitchProfile: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshDay() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    DashboardContent(state, onAnalyze, onRegisterMeal, onOpenDay, onSwitchProfile)
}

@Composable
fun DashboardContent(
    state: DashboardState,
    onAnalyze: () -> Unit,
    onRegisterMeal: (MealType?) -> Unit,
    onOpenDay: () -> Unit,
    onSwitchProfile: () -> Unit,
) {
    val data = state.data
    if (state.loading || data == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val target = data.target
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${Labels.greeting()}, ${data.profile.name}", style = MaterialTheme.typography.headlineMedium)
                    Text(Labels.day(state.today, state.today), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(onClick = onSwitchProfile, label = { Text("Cambiar perfil") })
            }
        }
        item {
            AppearIn(0) {
                NutriCard {
                    Text("CALORÍAS DE HOY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CalorieRing(consumed = state.consumed.energyKcal, target = target.effectiveKcal, size = 156.dp)
                        Spacer(Modifier.width(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Stat("Consumidas", Labels.approxKcal(state.consumed.energyKcal), Modifier.testTag("consumed"))
                            Stat("Objetivo", Labels.kcal(target.effectiveKcal.toDouble()))
                            val remaining = target.effectiveKcal - state.consumed.energyKcal
                            Stat(if (remaining >= 0) "Restantes" else "Por encima", Labels.kcal(kotlin.math.abs(remaining)))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    EstimateNote()
                }
            }
        }
        item {
            AppearIn(1) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onAnalyze,
                        modifier = Modifier.weight(1f).height(56.dp).testTag("analyze"),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text("📷  Analizar comida") }
                    OutlinedButton(
                        onClick = { onRegisterMeal(null) },
                        modifier = Modifier.weight(1f).height(56.dp).testTag("register"),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text("+ Registrar comida") }
                }
            }
        }
        item {
            AppearIn(2) {
                NutriCard {
                    Text("Macronutrientes", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    MacroBar("Proteínas", state.consumed.proteinG, target.macros.proteinG, ProteinColor)
                    Spacer(Modifier.height(12.dp))
                    MacroBar("Carbohidratos", state.consumed.carbohydratesG, target.macros.carbohydratesG, CarbsColor)
                    Spacer(Modifier.height(12.dp))
                    MacroBar("Grasas", state.consumed.fatG, target.macros.fatG, FatColor)
                }
            }
        }
        MealType.entries.forEachIndexed { index, type ->
            item(key = type.name) {
                AppearIn(3 + index) {
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
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun MealTypeCard(type: MealType, nutrients: Nutrients?, itemsSummary: String, onAdd: () -> Unit, onOpen: () -> Unit) {
    NutriCard(onClick = if (nutrients != null) onOpen else onAdd, contentPadding = PaddingValues(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Labels.mealEmoji(type), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(Labels.mealType(type), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (nutrients == null) "Sin registrar" else "${Labels.approxKcal(nutrients.energyKcal)} · $itemsSummary",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Añadir a ${Labels.mealType(type)}") }
        }
    }
}
