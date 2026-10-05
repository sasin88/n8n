package com.nutriai.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.nutriai.core.ui.components.EmptyState
import com.nutriai.core.ui.components.EstimateNote
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.core.ui.components.SecondaryButton
import com.nutriai.core.ui.components.ValueRow
import com.nutriai.core.ui.format.Labels
import com.nutriai.data.repository.Clock
import com.nutriai.domain.model.Meal
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.repository.MealRepository
import com.nutriai.feature.common.ActiveProfileTargets
import com.nutriai.navigation.DayDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DayDetailState(val date: LocalDate, val today: LocalDate, val meals: List<Meal> = emptyList(), val targetKcal: Int? = null)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DayDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    targets: ActiveProfileTargets,
    meals: MealRepository,
    clock: Clock,
) : ViewModel() {
    private val date = LocalDate.ofEpochDay(savedStateHandle.toRoute<DayDetailRoute>().epochDay)
    private val initial = DayDetailState(date, clock.today())

    val state: StateFlow<DayDetailState> = targets.flow.flatMapLatest { data ->
        if (data == null) flowOf(initial)
        else meals.observeMeals(data.profile.id, date, date).map { initial.copy(meals = it, targetKcal = data.target.effectiveKcal) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    onBack: () -> Unit,
    onOpenMeal: (Long) -> Unit,
    onAddMeal: (LocalDate) -> Unit,
    viewModel: DayDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val total = Nutrients.sum(state.meals.map { it.nutrients })
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Labels.day(state.date, state.today)) },
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
            item {
                NutriCard {
                    ValueRow("Calorías", Labels.approxKcal(total.energyKcal))
                    state.targetKcal?.let { ValueRow("Objetivo", Labels.kcal(it.toDouble())) }
                    ValueRow("Proteínas", Labels.grams(total.proteinG))
                    ValueRow("Carbohidratos", Labels.grams(total.carbohydratesG))
                    ValueRow("Grasas", Labels.grams(total.fatG))
                    ValueRow("Fibra", total.fiberG?.let(Labels::grams) ?: "sin datos completos")
                }
            }
            if (state.meals.isEmpty()) item { EmptyState("🍽️", "Sin comidas", "No registraste comidas este día.") }
            items(state.meals, key = { it.id }) { meal -> MealRow(meal) { onOpenMeal(meal.id) } }
            item { SecondaryButton("+ Registrar comida en este día", { onAddMeal(state.date) }) }
            item { EstimateNote("Toca una comida para editarla o eliminarla.") }
        }
    }
}
