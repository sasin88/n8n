package com.nutriai.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.core.ui.components.AppearIn
import com.nutriai.core.ui.components.BarChart
import com.nutriai.core.ui.components.EstimateNote
import com.nutriai.core.ui.components.LargeTitle
import com.nutriai.core.ui.components.LineChart
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.core.ui.components.SecondaryButton
import com.nutriai.core.ui.components.ValueRow
import com.nutriai.core.ui.format.Labels
import com.nutriai.core.ui.theme.ExerciseColor
import com.nutriai.core.ui.theme.WaterColor
import com.nutriai.data.repository.Clock
import com.nutriai.domain.health.BodyAnalysis
import com.nutriai.domain.health.Hydration
import com.nutriai.domain.model.DailySummary
import com.nutriai.domain.model.UnitSystem
import com.nutriai.domain.model.Units
import com.nutriai.domain.model.WeightRecord
import com.nutriai.domain.nutrition.NutritionStats
import com.nutriai.domain.repository.ExerciseRepository
import com.nutriai.domain.repository.MealRepository
import com.nutriai.domain.repository.WaterRepository
import com.nutriai.domain.repository.WeightRepository
import com.nutriai.feature.common.ActiveProfileTargets
import com.nutriai.feature.common.ProfileWithTarget
import com.nutriai.feature.onboarding.BmiGauge
import com.nutriai.feature.onboarding.bmiLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class ProgressState(
    val data: ProfileWithTarget? = null,
    val week: List<DailySummary> = emptyList(),
    val waterWeek: List<Int> = emptyList(),
    val exerciseWeek: List<Int> = emptyList(),
    val weights: List<WeightRecord> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgressViewModel @Inject constructor(
    targets: ActiveProfileTargets,
    meals: MealRepository,
    water: WaterRepository,
    exercise: ExerciseRepository,
    weights: WeightRepository,
    clock: Clock,
) : ViewModel() {
    val state: StateFlow<ProgressState> = targets.flow.flatMapLatest { data ->
        if (data == null) return@flatMapLatest flowOf(ProgressState())
        val today = clock.today()
        val from = today.minusDays(6)
        val id = data.profile.id
        combine(
            meals.observeMeals(id, from, today),
            water.observe(id, from, today),
            exercise.observe(id, from, today),
            weights.observeRecords(id),
        ) { m, w, e, wr ->
            val days = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.toList()
            ProgressState(
                data = data,
                week = NutritionStats.dailySummaries(m, from, today),
                waterWeek = days.map { d -> w.filter { it.date == d }.sumOf { it.ml } },
                exerciseWeek = days.map { d -> e.filter { it.date == d }.sumOf { it.kcal } },
                weights = wr,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())
}

@Composable
fun ProgressScreen(
    onOpenWeight: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenStats: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val data = state.data ?: return
    val p = data.profile
    val imperial = p.unitSystem == UnitSystem.IMPERIAL
    fun w(kg: Double) = if (imperial) "${Units.kgToLb(kg).roundToInt()} lb" else Labels.weight(kg)
    val dayLabels = state.week.map { it.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale("es")).uppercase() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { LargeTitle("Progreso", subtitle = "Tendencias") }
        item {
            AppearIn(0) {
                val bmi = BodyAnalysis.analyze(p.weightKg, p.heightCm)
                NutriCard {
                    Text("IMC", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(String.format("%.1f", bmi.bmi), style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.width(8.dp))
                        Text(bmiLabel(bmi.category), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    BmiGauge(bmi.bmi)
                    Spacer(Modifier.height(8.dp))
                    val healthy = bmi.healthyWeightKg
                    Text(
                        when {
                            p.weightKg > healthy.endInclusive -> "Te faltan ${w(p.weightKg - healthy.endInclusive)} para el rango saludable."
                            p.weightKg < healthy.start -> "Estás ${w(healthy.start - p.weightKg)} por debajo del rango saludable."
                            else -> "Estás dentro del rango saludable para tu altura."
                        },
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            AppearIn(1) {
                NutriCard(onClick = onOpenWeight) {
                    Text("Peso", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(w(p.weightKg), style = MaterialTheme.typography.displaySmall)
                        p.targetWeightKg?.let {
                            Spacer(Modifier.width(8.dp))
                            Text("meta ${w(it)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val recent = state.weights.takeLast(30)
                    if (recent.size >= 2) {
                        Spacer(Modifier.height(10.dp))
                        LineChart(recent.map { it.weightKg }, height = 150.dp)
                        val trend = NutritionStats.weightTrendKgPerWeek(recent.takeLast(12))
                        trend?.let {
                            Text(
                                if (abs(it) < 0.05) "Estable" else if (it < 0) "↓ ${String.format("%.2f", -it)} kg por semana" else "↑ ${String.format("%.2f", it)} kg por semana",
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Text("Registra tu peso al menos dos veces para ver tu evolución.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            AppearIn(2) {
                NutriCard {
                    Text("Calorías · últimos 7 días", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    BarChart(state.week.map { it.nutrients.energyKcal }, dayLabels, target = data.target.effectiveKcal.toDouble())
                    Spacer(Modifier.height(6.dp))
                    val avg = NutritionStats.averageOfLoggedDays(state.week)
                    ValueRow("Promedio (días registrados)", avg?.let { Labels.approxKcal(it.energyKcal) } ?: "—")
                    ValueRow("Objetivo", Labels.kcal(data.target.effectiveKcal.toDouble()))
                }
            }
        }
        item {
            AppearIn(3) {
                NutriCard {
                    Text("Agua · últimos 7 días", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    val goal = p.waterGoalMl ?: Hydration.suggestedGoalMl(p.weightKg)
                    BarChart(state.waterWeek.map { it.toDouble() }, dayLabels, target = goal.toDouble(), color = WaterColor, height = 120.dp)
                    ValueRow("Meta diaria", "${Labels.formatNumber(goal / 1000.0)} L")
                }
            }
        }
        item {
            AppearIn(4) {
                NutriCard {
                    Text("Ejercicio · últimos 7 días", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    BarChart(state.exerciseWeek.map { it.toDouble() }, dayLabels, color = ExerciseColor, height = 120.dp)
                    ValueRow("Total de la semana", "${state.exerciseWeek.sum()} kcal")
                }
            }
        }
        item { SecondaryButton("Historial día a día", onOpenHistory) }
        item { SecondaryButton("Estadísticas detalladas", onOpenStats) }
        item { EstimateNote("Tendencias basadas en estimaciones. No son un diagnóstico.") }
    }
}
