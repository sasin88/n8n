package com.nutriai.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutriai.core.ui.components.AppearIn
import com.nutriai.core.ui.components.BarChart
import com.nutriai.core.ui.components.EmptyState
import com.nutriai.core.ui.components.EstimateNote
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.core.ui.components.ValueRow
import com.nutriai.core.ui.format.Labels
import com.nutriai.domain.model.Meal
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenDay: (LocalDate) -> Unit,
    onOpenMeal: (Long) -> Unit,
    onOpenStats: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            com.nutriai.core.ui.components.GlassIconButton(onClick = onBack, contentDescription = "Volver") {
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, null)
            }
            com.nutriai.core.ui.components.LargeTitle("Historial") {
                TextButton(onClick = onOpenStats) { Text("Estadísticas") }
            }
        }
        item {
            val periods = HistoryPeriod.entries
            com.nutriai.core.ui.components.GlassSegmented(
                options = listOf("Día", "Semana", "Mes"),
                selectedIndex = periods.indexOf(state.period),
                onSelect = { viewModel.setPeriod(periods[it]) },
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.move(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Anterior") }
                Text(periodTitle(state), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.move(1) }, enabled = state.canGoForward) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Siguiente")
                }
            }
        }
        val target = state.data?.target?.effectiveKcal
        if (state.period == HistoryPeriod.DAY) {
            val total = state.summaries.firstOrNull()?.nutrients
            item {
                AppearIn {
                    NutriCard {
                        ValueRow("Calorías", total?.let { Labels.approxKcal(it.energyKcal) } ?: "—")
                        ValueRow("Objetivo", target?.let { Labels.kcal(it.toDouble()) } ?: "—")
                        ValueRow("Proteínas", total?.let { Labels.grams(it.proteinG) } ?: "—")
                        ValueRow("Carbohidratos", total?.let { Labels.grams(it.carbohydratesG) } ?: "—")
                        ValueRow("Grasas", total?.let { Labels.grams(it.fatG) } ?: "—")
                        state.weights.lastOrNull()?.let { ValueRow("Peso registrado", Labels.weight(it.weightKg)) }
                    }
                }
            }
            if (state.meals.isEmpty()) {
                item { EmptyState("🍽️", "Sin comidas este día", "Las comidas que registres aparecerán aquí.") }
            } else {
                items(state.meals, key = { it.id }) { meal -> MealRow(meal) { onOpenMeal(meal.id) } }
            }
        } else {
            item {
                AppearIn {
                    NutriCard {
                        Text("Calorías por día", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        BarChart(
                            values = state.summaries.map { it.nutrients.energyKcal },
                            labels = if (state.period == HistoryPeriod.WEEK) {
                                state.summaries.map { it.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale("es")).uppercase() }
                            } else {
                                emptyList()
                            },
                            target = target?.toDouble(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Línea discontinua: tu objetivo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                NutriCard {
                    Text("Promedio diario (días con registro)", style = MaterialTheme.typography.titleMedium)
                    val avg = state.average
                    ValueRow("Calorías", avg?.let { Labels.approxKcal(it.energyKcal) } ?: "—")
                    ValueRow("Proteínas", avg?.let { Labels.grams(it.proteinG) } ?: "—")
                    ValueRow("Carbohidratos", avg?.let { Labels.grams(it.carbohydratesG) } ?: "—")
                    ValueRow("Grasas", avg?.let { Labels.grams(it.fatG) } ?: "—")
                    ValueRow("Días registrados", "${state.summaries.count { it.mealCount > 0 }} de ${state.summaries.size}")
                    if (state.weights.isNotEmpty()) {
                        ValueRow("Peso", "${Labels.weight(state.weights.first().weightKg)} → ${Labels.weight(state.weights.last().weightKg)}")
                    }
                }
            }
            item { Text("Días", style = MaterialTheme.typography.titleMedium) }
            items(state.summaries.filter { it.mealCount > 0 }.reversed(), key = { it.date.toEpochDay() }) { day ->
                NutriCard(onClick = { onOpenDay(day.date) }, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Labels.day(day.date, state.today), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(Labels.approxKcal(day.nutrients.energyKcal), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        item { EstimateNote() }
    }
}

@Composable
fun MealRow(meal: Meal, onClick: () -> Unit) {
    NutriCard(onClick = onClick, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Labels.mealEmoji(meal.type), style = MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(Labels.mealType(meal.type), style = MaterialTheme.typography.titleMedium)
                Text(
                    meal.items.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Text(Labels.approxKcal(meal.nutrients.energyKcal), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun periodTitle(state: HistoryState): String = when (state.period) {
    HistoryPeriod.DAY -> Labels.day(state.anchor, state.today)
    HistoryPeriod.WEEK -> "${Labels.shortDate(state.from)} – ${Labels.shortDate(state.to)}"
    HistoryPeriod.MONTH -> state.from.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("es")).replaceFirstChar { it.uppercase() } + " ${state.from.year}"
}
