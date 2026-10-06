package com.nutriai.feature.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.core.ui.components.AppearIn
import com.nutriai.core.ui.components.EmptyState
import com.nutriai.core.ui.components.LineChart
import com.nutriai.core.ui.components.NoticeCard
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.core.ui.components.PrimaryButton
import com.nutriai.core.ui.components.ValueRow
import com.nutriai.core.ui.format.Labels
import com.nutriai.data.repository.Clock
import com.nutriai.domain.model.UserProfile
import com.nutriai.domain.model.WeightRecord
import com.nutriai.domain.nutrition.NutritionStats
import com.nutriai.domain.repository.ProfileRepository
import com.nutriai.domain.repository.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeightState(val profile: UserProfile? = null, val records: List<WeightRecord> = emptyList(), val today: LocalDate = LocalDate.now()) {
    val trend: Double? get() = NutritionStats.weightTrendKgPerWeek(records.takeLast(12))
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WeightViewModel @Inject constructor(
    profiles: ProfileRepository,
    private val weights: WeightRepository,
    private val clock: Clock,
) : ViewModel() {
    val state: StateFlow<WeightState> = profiles.observeActiveProfile().flatMapLatest { profile ->
        if (profile == null) flowOf(WeightState())
        else weights.observeRecords(profile.id).map { WeightState(profile, it, clock.today()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightState())

    fun add(kg: Double) = viewModelScope.launch {
        val profile = state.value.profile ?: return@launch
        runCatching { weights.add(WeightRecord(profileId = profile.id, date = clock.today(), weightKg = kg)) }
    }

    fun delete(id: Long) = viewModelScope.launch { runCatching { weights.delete(id) } }
}

@Composable
fun WeightScreen(onBack: () -> Unit = {}, viewModel: WeightViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            com.nutriai.core.ui.components.GlassIconButton(onClick = onBack, contentDescription = "Volver") {
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, null)
            }
            com.nutriai.core.ui.components.LargeTitle("Peso")
        }
        item {
            AppearIn {
                NutriCard {
                    ValueRow("Peso actual", state.profile?.let { Labels.weight(it.weightKg) } ?: "—")
                    state.profile?.targetWeightKg?.let { ValueRow("Peso objetivo", Labels.weight(it)) }
                    val trend = state.trend
                    ValueRow(
                        "Tendencia",
                        when {
                            trend == null -> "Registra tu peso al menos 2 veces"
                            abs(trend) < 0.05 -> "Estable"
                            trend < 0 -> "↓ ${String.format("%.2f", -trend)} kg/semana"
                            else -> "↑ ${String.format("%.2f", trend)} kg/semana"
                        },
                    )
                    if (state.records.size >= 2) {
                        Spacer(Modifier.height(12.dp))
                        LineChart(state.records.takeLast(30).map { it.weightKg })
                    }
                }
            }
        }
        item { PrimaryButton("+ Registrar peso de hoy", { adding = true }) }
        if (state.records.isEmpty()) {
            item { EmptyState("⚖️", "Sin registros todavía", "Registra tu peso de vez en cuando para ver tu evolución.") }
        }
        items(state.records.reversed(), key = { it.id }) { record ->
            NutriCard(contentPadding = PaddingValues(start = 20.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Labels.day(record.date, state.today), modifier = Modifier.weight(1f))
                    Text(Labels.weight(record.weightKg), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { viewModel.delete(record.id) }) { Icon(Icons.Default.Delete, "Eliminar registro") }
                }
            }
        }
        item {
            NoticeCard("Los cambios de peso de un día a otro son normales. Fíjate en la tendencia de varias semanas.")
        }
    }

    if (adding) {
        var text by remember { mutableStateOf(state.profile?.weightKg?.let(Labels::plain) ?: "") }
        val value = text.replace(',', '.').toDoubleOrNull()?.takeIf { it in 20.0..400.0 }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Peso de hoy") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { s -> text = s.filter { it.isDigit() || it == '.' || it == ',' }.take(6) },
                    suffix = { Text("kg") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(enabled = value != null, onClick = { value?.let(viewModel::add); adding = false }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancelar") } },
        )
    }
}
