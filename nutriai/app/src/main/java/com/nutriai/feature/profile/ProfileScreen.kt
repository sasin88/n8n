package com.nutriai.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.core.ui.components.AppearIn
import com.nutriai.core.ui.components.NoticeCard
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.core.ui.components.SecondaryButton
import com.nutriai.core.ui.components.ValueRow
import com.nutriai.core.ui.format.Labels
import com.nutriai.domain.nutrition.CalorieTargetCalculator
import com.nutriai.domain.repository.ProfileRepository
import com.nutriai.feature.common.ActiveProfileTargets
import com.nutriai.feature.common.ProfileWithTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    targets: ActiveProfileTargets,
    private val profiles: ProfileRepository,
) : ViewModel() {
    val state: StateFlow<ProfileWithTarget?> = targets.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** null vuelve al objetivo calculado. */
    fun setManualTarget(kcal: Int?) = viewModelScope.launch {
        val profile = state.value?.profile ?: return@launch
        runCatching { profiles.update(profile.copy(manualCalorieTarget = kcal)) }
    }
}

@Composable
fun ProfileScreen(
    onEdit: (Long) -> Unit,
    onSwitch: () -> Unit,
    onSettings: () -> Unit,
    onPrivacy: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val data by viewModel.state.collectAsStateWithLifecycle()
    var editingTarget by remember { mutableStateOf(false) }
    val current = data ?: return
    val p = current.profile
    val t = current.target

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text(p.name, style = MaterialTheme.typography.headlineMedium) }
        item {
            AppearIn(0) {
                NutriCard {
                    ValueRow("Sexo", Labels.sex(p.sex))
                    ValueRow("Edad", "${p.ageYears} años")
                    ValueRow("Peso", Labels.weight(p.weightKg))
                    ValueRow("Altura", "${Labels.formatNumber(p.heightCm)} cm")
                    ValueRow("Actividad", Labels.activityShort(p.activityLevel))
                    ValueRow("Objetivo", Labels.goal(p.goal))
                    p.targetWeightKg?.let { ValueRow("Peso objetivo", Labels.weight(it)) }
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton("Editar perfil", { onEdit(p.id) })
                }
            }
        }
        item {
            AppearIn(1) {
                NutriCard {
                    Text("Tu objetivo diario", style = MaterialTheme.typography.titleMedium)
                    ValueRow("Metabolismo basal (BMR)", Labels.approxKcal(t.bmrKcal))
                    ValueRow("Gasto diario estimado (TDEE)", Labels.approxKcal(t.tdeeKcal))
                    ValueRow("Objetivo sugerido", Labels.kcal(t.suggestedKcal.toDouble()))
                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ValueRow(
                        if (t.isManual) "Objetivo usado (manual)" else "Objetivo usado",
                        Labels.kcal(t.effectiveKcal.toDouble()),
                        onClick = { editingTarget = true },
                    )
                    TextButton(onClick = { editingTarget = true }) { Text("Ajustar objetivo manualmente") }
                    if (t.minimumApplied) {
                        NoticeCard(
                            "El cálculo daba un objetivo muy bajo, así que usamos un mínimo orientativo. Consulta con un profesional para un plan personalizado.",
                            isWarning = true,
                        )
                    }
                    if (t.manualBelowMinimum) {
                        NoticeCard(
                            "Tu objetivo manual está por debajo de ${CalorieTargetCalculator.minimumKcal(p.sex)} kcal. Las dietas muy bajas en calorías deben seguirse con supervisión profesional.",
                            isWarning = true,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Cálculo estimado con fórmulas reconocidas. No es un consejo médico. Si tienes una condición de salud, estás embarazada o en lactancia, consulta con un profesional.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton("Cambiar de perfil", onSwitch)
                SecondaryButton("Configuración", onSettings)
                SecondaryButton("Privacidad y datos", onPrivacy)
            }
        }
    }

    if (editingTarget) {
        var text by remember { mutableStateOf(t.manualCalorieTargetText()) }
        val value = text.toIntOrNull()?.takeIf { it in 800..6000 }
        AlertDialog(
            onDismissRequest = { editingTarget = false },
            title = { Text("Objetivo diario") },
            text = {
                Column {
                    Text("Sugerido: ${Labels.kcal(t.suggestedKcal.toDouble())}. Escribe tu propio objetivo o vuelve al sugerido.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { s -> text = s.filter(Char::isDigit).take(4) },
                        suffix = { Text("kcal") },
                        singleLine = true,
                        supportingText = { Text("Entre 800 y 6000 kcal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = value != null, onClick = { viewModel.setManualTarget(value); editingTarget = false }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setManualTarget(null); editingTarget = false }) { Text("Usar sugerido") }
            },
        )
    }
}

private fun com.nutriai.domain.nutrition.CalorieTarget.manualCalorieTargetText() = effectiveKcal.toString()
