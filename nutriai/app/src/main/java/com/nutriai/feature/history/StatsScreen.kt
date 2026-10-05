package com.nutriai.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.core.ui.components.ColorDot
import com.nutriai.core.ui.components.EstimateNote
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.core.ui.components.ValueRow
import com.nutriai.core.ui.format.Labels
import com.nutriai.core.ui.theme.CarbsColor
import com.nutriai.core.ui.theme.FatColor
import com.nutriai.core.ui.theme.ProteinColor
import com.nutriai.data.repository.Clock
import com.nutriai.domain.model.DailySummary
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.nutrition.CalorieTargetCalculator
import com.nutriai.domain.nutrition.NutritionStats
import com.nutriai.domain.repository.MealRepository
import com.nutriai.domain.repository.WeightRepository
import com.nutriai.feature.common.ActiveProfileTargets
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class StatsState(
    val week: List<DailySummary> = emptyList(),
    val month: List<DailySummary> = emptyList(),
    val targetKcal: Int? = null,
    val weightTrendKgPerWeek: Double? = null,
    val weightCount: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    targets: ActiveProfileTargets,
    meals: MealRepository,
    weights: WeightRepository,
    clock: Clock,
) : ViewModel() {
    val state: StateFlow<StatsState> = targets.flow.flatMapLatest { data ->
        if (data == null) return@flatMapLatest flowOf(StatsState())
        val today = clock.today()
        val from = today.minusDays(29)
        combine(meals.observeMeals(data.profile.id, from, today), weights.observeRecords(data.profile.id)) { list, records ->
            val month = NutritionStats.dailySummaries(list, from, today)
            val recent = records.filter { !it.date.isBefore(today.minusDays(60)) }
            StatsState(
                week = month.takeLast(7),
                month = month,
                targetKcal = data.target.effectiveKcal,
                weightTrendKgPerWeek = NutritionStats.weightTrendKgPerWeek(recent),
                weightCount = recent.size,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit, viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { PeriodCard("Últimos 7 días", state.week, state.targetKcal) }
            item { PeriodCard("Últimos 30 días", state.month, state.targetKcal) }
            item {
                NutriCard {
                    Text("Reparto de calorías (30 días)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    val avg = NutritionStats.averageOfLoggedDays(state.month)
                    if (avg == null) {
                        Text("Aún no hay datos suficientes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        MacroShare(avg)
                    }
                }
            }
            item {
                NutriCard {
                    Text("Peso", style = MaterialTheme.typography.titleMedium)
                    val trend = state.weightTrendKgPerWeek
                    ValueRow(
                        "Tendencia",
                        when {
                            trend == null -> "Necesitas al menos 2 registros"
                            abs(trend) < 0.05 -> "Estable"
                            trend < 0 -> "Bajando ${String.format("%.2f", -trend)} kg/semana"
                            else -> "Subiendo ${String.format("%.2f", trend)} kg/semana"
                        },
                    )
                    ValueRow("Registros (60 días)", state.weightCount.toString())
                }
            }
            item { EstimateNote("Estadísticas basadas en estimaciones. No son un diagnóstico.") }
        }
    }
}

@Composable
private fun PeriodCard(title: String, days: List<DailySummary>, target: Int?) {
    val logged = days.filter { it.mealCount > 0 }
    val avg = NutritionStats.averageOfLoggedDays(days)
    // Días dentro de ±10 % del objetivo.
    val onTarget = if (target != null) logged.count { abs(it.nutrients.energyKcal - target) <= target * 0.10 } else 0
    NutriCard {
        Text(title, style = MaterialTheme.typography.titleMedium)
        ValueRow("Días registrados", "${logged.size} de ${days.size}")
        ValueRow("Promedio diario", avg?.let { Labels.approxKcal(it.energyKcal) } ?: "—")
        if (target != null) ValueRow("Días cerca del objetivo (±10%)", "$onTarget")
        ValueRow("Proteínas promedio", avg?.let { Labels.grams(it.proteinG) } ?: "—")
    }
}

@Composable
private fun MacroShare(avg: Nutrients) {
    val p = avg.proteinG * CalorieTargetCalculator.KCAL_PER_G_PROTEIN
    val c = avg.carbohydratesG * CalorieTargetCalculator.KCAL_PER_G_CARBS
    val f = avg.fatG * CalorieTargetCalculator.KCAL_PER_G_FAT
    val sum = (p + c + f).takeIf { it > 0 } ?: 1.0
    listOf(Triple("Proteínas", p, ProteinColor), Triple("Carbohidratos", c, CarbsColor), Triple("Grasas", f, FatColor)).forEach { (label, kcal, color) ->
        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            ColorDot(color)
            Spacer(Modifier.width(10.dp))
            Text(label, modifier = Modifier.weight(1f))
            Text(Labels.percent(kcal / sum), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
