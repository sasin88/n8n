package com.nutriai.feature.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutriai.core.ui.components.NoticeCard
import com.nutriai.core.ui.components.PrimaryButton
import com.nutriai.core.ui.components.SectionTitle
import com.nutriai.core.ui.format.Labels
import com.nutriai.domain.model.ActivityLevel
import com.nutriai.domain.model.Goal
import com.nutriai.domain.model.Sex

@Composable
fun ProfileFormScreen(
    onSaved: () -> Unit,
    onBack: (() -> Unit)?,
    viewModel: ProfileFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    ProfileFormContent(state = state, onChange = viewModel::update, onSave = viewModel::save, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileFormContent(
    state: ProfileFormState,
    onChange: ((ProfileFormState) -> ProfileFormState) -> Unit,
    onSave: () -> Unit,
    onBack: (() -> Unit)?,
) {
    // Los errores se muestran tras el primer intento de guardar, no mientras se escribe.
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var showOptional by rememberSaveable { mutableStateOf(state.targetWeight.isNotBlank()) }
    val v = state.validation
    val complete = state.sex != null && state.activity != null && state.goal != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Editar perfil" else "Tu perfil") },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Solo lo necesario para estimar tu objetivo diario. Se guarda únicamente en este teléfono.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.name,
                onValueChange = { text -> onChange { it.copy(name = text) } },
                label = { Text("Nombre o apodo") },
                singleLine = true,
                isError = showErrors && v.nameError != null,
                supportingText = { if (showErrors && v.nameError != null) Text(v.nameError) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth().testTag("name"),
            )

            SectionTitle("Sexo")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sex.entries.forEach { sex ->
                    FilterChip(selected = state.sex == sex, onClick = { onChange { it.copy(sex = sex) } }, label = { Text(Labels.sex(sex)) })
                }
            }
            Text(
                "Se usa solo para la fórmula de gasto energético.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Edad", state.age, "años", showErrors, v.ageError, Modifier.weight(1f).testTag("age"), KeyboardType.Number) { t -> onChange { it.copy(age = t) } }
                NumberField("Peso", state.weight, "kg", showErrors, v.weightError, Modifier.weight(1f).testTag("weight")) { t -> onChange { it.copy(weight = t) } }
                NumberField("Altura", state.height, "cm", showErrors, v.heightError, Modifier.weight(1f).testTag("height")) { t -> onChange { it.copy(height = t) } }
            }
            if (v.isMinor) {
                NoticeCard(
                    "Si eres menor de 18 años, las necesidades nutricionales son distintas. Consulta con un profesional de la salud antes de seguir un objetivo calórico.",
                    isWarning = true,
                )
            }

            SectionTitle("¿Cuánto te mueves?")
            ActivityLevel.entries.forEach { level ->
                Row(
                    Modifier.fillMaxWidth().clickable { onChange { it.copy(activity = level) } }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = state.activity == level, onClick = { onChange { it.copy(activity = level) } })
                    Text(Labels.activity(level), style = MaterialTheme.typography.bodyLarge)
                }
            }

            SectionTitle("Tu objetivo")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Goal.entries.forEach { goal ->
                    FilterChip(selected = state.goal == goal, onClick = { onChange { it.copy(goal = goal) } }, label = { Text(Labels.goal(goal)) })
                }
            }

            TextButton(onClick = { showOptional = !showOptional }) {
                Text(if (showOptional) "Ocultar opciones adicionales" else "Opciones adicionales (opcional)")
            }
            AnimatedVisibility(showOptional) {
                NumberField("Peso objetivo", state.targetWeight, "kg", showErrors, v.targetWeightError, Modifier.fillMaxWidth()) { t ->
                    onChange { it.copy(targetWeight = t) }
                }
            }

            state.error?.let { NoticeCard(it, isWarning = true) }
            if (showErrors && !complete) {
                NoticeCard("Elige sexo, nivel de actividad y objetivo para continuar.", isWarning = true)
            }
            Spacer(Modifier.height(4.dp))
            PrimaryButton(
                text = if (state.isEditing) "Guardar cambios" else "Crear perfil",
                onClick = {
                    showErrors = true
                    if (v.isValid && complete) onSave()
                },
                enabled = !state.saving,
                modifier = Modifier.testTag("save_profile"),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    suffix: String,
    showErrors: Boolean,
    error: String?,
    modifier: Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    onValueChange: (String) -> Unit,
) {
    val hasError = showErrors && error != null
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onValueChange(text.filter { it.isDigit() || it == '.' || it == ',' }.take(6)) },
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        isError = hasError,
        supportingText = { if (hasError) Text(error!!) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
    )
}
