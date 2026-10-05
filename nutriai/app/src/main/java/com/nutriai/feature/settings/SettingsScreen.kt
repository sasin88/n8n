package com.nutriai.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.nutriai.BuildConfig
import com.nutriai.core.ui.components.NoticeCard
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.core.ui.components.ValueRow
import com.nutriai.core.ui.format.Labels
import com.nutriai.data.settings.AppSettings
import com.nutriai.data.settings.SettingsRepository
import com.nutriai.domain.ai.FoodAnalysisService
import com.nutriai.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    service: FoodAnalysisService,
) : ViewModel() {
    val state: StateFlow<AppSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val aiProvider = service.providerName
    val aiIsMock = service.isMock

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setFormula(id: String) = viewModelScope.launch { settings.setEnergyFormula(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
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
                    Text("Apariencia", style = MaterialTheme.typography.titleMedium)
                    ThemeMode.entries.forEach { mode ->
                        Choice(Labels.themeMode(mode), state.themeMode == mode) { viewModel.setTheme(mode) }
                    }
                }
            }
            item {
                NutriCard {
                    Text("Fórmula de cálculo", style = MaterialTheme.typography.titleMedium)
                    Choice("Mifflin-St Jeor (recomendada)", state.energyFormulaId == "mifflin_st_jeor") { viewModel.setFormula("mifflin_st_jeor") }
                    Choice("Harris-Benedict revisada", state.energyFormulaId == "harris_benedict_revised") { viewModel.setFormula("harris_benedict_revised") }
                    Text(
                        "Ambas son estimaciones poblacionales. La diferencia suele ser pequeña.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                NutriCard {
                    Text("Análisis con IA", style = MaterialTheme.typography.titleMedium)
                    ValueRow("Proveedor", viewModel.aiProvider)
                    if (viewModel.aiIsMock) {
                        NoticeCard("La IA real no está conectada: el análisis de fotos funciona en modo demostración con resultados simulados.", isWarning = true)
                    }
                }
            }
            item {
                NutriCard {
                    Text("Acerca de", style = MaterialTheme.typography.titleMedium)
                    ValueRow("Versión", BuildConfig.VERSION_NAME)
                    ValueRow("Datos nutricionales", "USDA FoodData Central")
                    Text(
                        "NutriAI ofrece estimaciones orientativas y no sustituye a un médico, nutricionista o dietista.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

