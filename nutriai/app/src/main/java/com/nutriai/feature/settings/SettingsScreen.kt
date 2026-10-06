package com.nutriai.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import com.nutriai.BuildConfig
import com.nutriai.core.ui.components.GlassIconButton
import com.nutriai.core.ui.components.GlassSegmented
import com.nutriai.core.ui.components.LargeTitle
import com.nutriai.core.ui.components.NoticeCard
import com.nutriai.core.ui.format.Labels
import com.nutriai.data.settings.AppSettings
import com.nutriai.data.settings.SettingsRepository
import com.nutriai.domain.ai.FoodAnalysisService
import com.nutriai.domain.health.Hydration
import com.nutriai.domain.model.ThemeMode
import com.nutriai.domain.model.UnitSystem
import com.nutriai.domain.model.UserProfile
import com.nutriai.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val profiles: ProfileRepository,
    service: FoodAnalysisService,
) : ViewModel() {
    val state: StateFlow<AppSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val profile: StateFlow<UserProfile?> = profiles.observeActiveProfile().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val aiProvider = service.providerName
    val aiIsMock = service.isMock

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setFormula(id: String) = viewModelScope.launch { settings.setEnergyFormula(id) }
    fun updateProfile(transform: (UserProfile) -> UserProfile) = viewModelScope.launch {
        profile.value?.let { runCatching { profiles.update(transform(it)) } }
    }
}

/** Preferencias: unidades, presupuesto, agua, apariencia, fórmula e información de la app. */
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    var editingWater by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            GlassIconButton(onClick = onBack, contentDescription = "Volver") { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
            LargeTitle("Preferencias")
        }
        profile?.let { p ->
            item {
                GlassGroup("Unidades") {
                    GroupRow("Sistema", showDivider = false, trailing = {
                        GlassSegmented(
                            listOf("kg · cm", "lb · ft"),
                            if (p.unitSystem == UnitSystem.METRIC) 0 else 1,
                            { i -> viewModel.updateProfile { it.copy(unitSystem = if (i == 0) UnitSystem.METRIC else UnitSystem.IMPERIAL) } },
                            modifier = Modifier.fillMaxWidth(0.6f),
                        )
                    })
                }
            }
            item {
                GlassGroup("Presupuesto diario") {
                    GroupSwitch("Sumar calorías del ejercicio", p.exerciseAddsToBudget, { v -> viewModel.updateProfile { it.copy(exerciseAddsToBudget = v) } }, "🔥")
                    GroupRow(
                        "Meta de agua",
                        value = "${Labels.formatNumber((p.waterGoalMl ?: Hydration.suggestedGoalMl(p.weightKg)) / 1000.0)} L" + if (p.waterGoalMl == null) " (auto)" else "",
                        emoji = "💧", showDivider = false, onClick = { editingWater = true },
                    )
                }
            }
        }
        item {
            GlassGroup("Apariencia") {
                ThemeMode.entries.forEachIndexed { i, mode ->
                    GroupRow(Labels.themeMode(mode), value = if (state.themeMode == mode) "✓" else null, showDivider = i < ThemeMode.entries.size - 1, onClick = { viewModel.setTheme(mode) }, trailing = {})
                }
            }
        }
        item {
            GlassGroup("Fórmula de cálculo") {
                GroupRow("Mifflin-St Jeor (recomendada)", value = if (state.energyFormulaId == "mifflin_st_jeor") "✓" else null, onClick = { viewModel.setFormula("mifflin_st_jeor") }, trailing = {})
                GroupRow("Harris-Benedict revisada", value = if (state.energyFormulaId == "harris_benedict_revised") "✓" else null, showDivider = false, onClick = { viewModel.setFormula("harris_benedict_revised") }, trailing = {})
            }
        }
        item {
            GlassGroup("Análisis con IA") {
                GroupRow("Proveedor", value = viewModel.aiProvider, showDivider = false)
            }
            if (viewModel.aiIsMock) {
                NoticeCard("La IA real no está conectada: el análisis de fotos funciona en modo demostración con resultados simulados.", isWarning = true, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            GlassGroup("Acerca de") {
                GroupRow("Versión", value = BuildConfig.VERSION_NAME)
                GroupRow("Datos nutricionales", value = "USDA FoodData Central")
                GroupRow("Idioma", value = "Español", showDivider = false)
            }
            Text(
                "NutriAI ofrece estimaciones orientativas y no sustituye a un médico, nutricionista o dietista.",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (editingWater) {
        val p = profile
        var text by remember { mutableStateOf(((p?.waterGoalMl ?: p?.let { Hydration.suggestedGoalMl(it.weightKg) } ?: 2000)).toString()) }
        val value = text.toIntOrNull()?.takeIf { it in 500..6000 }
        AlertDialog(
            onDismissRequest = { editingWater = false },
            title = { Text("Meta de agua diaria") },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { s -> text = s.filter(Char::isDigit).take(4) },
                    suffix = { Text("ml") }, singleLine = true, supportingText = { Text("Entre 500 y 6000 ml") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = { TextButton(enabled = value != null, onClick = { viewModel.updateProfile { it.copy(waterGoalMl = value) }; editingWater = false }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { viewModel.updateProfile { it.copy(waterGoalMl = null) }; editingWater = false }) { Text("Automática") } },
        )
    }
}
