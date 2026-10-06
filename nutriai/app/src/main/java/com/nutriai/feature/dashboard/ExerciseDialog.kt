package com.nutriai.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nutriai.core.ui.components.GlassChip
import com.nutriai.core.ui.format.Labels
import com.nutriai.domain.activity.ExerciseCalculator
import com.nutriai.domain.activity.ExerciseType

/** Registro de ejercicio: estimación por MET o calorías conocidas (p. ej. de un reloj). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseDialog(weightKg: Double, onDismiss: () -> Unit, onSave: (ExerciseType, Int, Int?) -> Unit) {
    var type by remember { mutableStateOf(ExerciseType.WALKING) }
    var minutesText by remember { mutableStateOf("30") }
    var kcalText by remember { mutableStateOf("") }
    val minutes = minutesText.toIntOrNull()?.takeIf { it in 1..1440 }
    val manualKcal = kcalText.toIntOrNull()?.takeIf { it in 0..10_000 }
    val estimate = minutes?.let { ExerciseCalculator.estimateKcal(type, it, weightKg) }
    val canSave = minutes != null && (manualKcal != null || estimate != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar ejercicio") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExerciseType.entries.forEach { t ->
                        GlassChip("${Labels.exerciseEmoji(t)} ${Labels.exercise(t)}", t == type, { type = t })
                    }
                }
                OutlinedTextField(
                    value = minutesText, onValueChange = { s -> minutesText = s.filter(Char::isDigit).take(4) },
                    label = { Text("Duración") }, suffix = { Text("min") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("exercise_minutes"),
                )
                OutlinedTextField(
                    value = kcalText, onValueChange = { s -> kcalText = s.filter(Char::isDigit).take(5) },
                    label = { Text("Calorías (opcional)") }, suffix = { Text("kcal") }, singleLine = true,
                    placeholder = { estimate?.let { Text("≈ $it") } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    when {
                        manualKcal != null -> "Se guardarán las calorías que indicaste."
                        estimate != null -> "Estimación ≈ $estimate kcal con tu peso (valores MET de referencia). Si tu reloj te da otra cifra, escríbela."
                        else -> "Para \"Otro\" indica las calorías."
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = canSave, onClick = { onSave(type, minutes!!, manualKcal) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
