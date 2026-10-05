package com.nutriai.feature.meal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nutriai.core.ui.components.NoticeCard
import com.nutriai.core.ui.components.PrimaryButton
import com.nutriai.core.ui.format.Labels
import com.nutriai.domain.model.FoodReference
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.nutrition.PortionConverter
import kotlinx.coroutines.delay

/** Hoja inferior para buscar un alimento en la base nutricional. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodPickerSheet(
    initialQuery: String,
    search: suspend (String) -> List<FoodReference>,
    onPicked: (FoodReference) -> Unit,
    onCreateCustom: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<FoodReference>>(emptyList()) }
    LaunchedEffect(query) {
        delay(150)
        results = search(query)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).imePadding()) {
            Text("¿Qué comiste?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Ej.: arroz, gallo pinto, huevo…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("food_search"),
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.heightIn(max = 460.dp)) {
                items(results, key = { it.id }) { food ->
                    Column(Modifier.fillMaxWidth().clickable { onPicked(food) }.padding(vertical = 12.dp)) {
                        Text(food.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${Labels.kcal(food.nutrientsPerBasis.energyKcal)} por 100 g · ${Labels.source(food.source)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                }
                item {
                    TextButton(onClick = { onCreateCustom(query) }, modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(if (results.isEmpty()) "No está en la lista: crear alimento" else "¿No lo encuentras? Crear alimento")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Elegir cantidad y unidad. Muestra al momento la estimación de calorías. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuantityDialog(
    food: FoodReference,
    initialQuantity: Double?,
    initialUnit: PortionUnit?,
    onConfirm: (Double, PortionUnit) -> Unit,
    onDismiss: () -> Unit,
) {
    val units = remember(food) { PortionConverter.supportedUnits(food) }
    var unit by remember { mutableStateOf(initialUnit?.takeIf { it in units } ?: MealDraft.defaultUnit(food)) }
    var text by remember { mutableStateOf(initialQuantity?.let(Labels::plain) ?: "") }
    val quantity = text.replace(',', '.').toDoubleOrNull()
    val preview = quantity?.let { PortionConverter.buildItem(food, it, unit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Cuánto ${food.name.lowercase()}?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    units.forEach { u ->
                        FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(Labels.unitName(u)) })
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickOptions(unit).forEach { option ->
                        FilterChip(
                            selected = quantity == option,
                            onClick = { text = Labels.plain(option) },
                            label = { Text(Labels.quantity(option, unit)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { new -> text = new.filter { it.isDigit() || it == '.' || it == ',' }.take(7) },
                    label = { Text("Introducir cantidad") },
                    suffix = { Text(Labels.unit(unit, quantity ?: 2.0)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("quantity"),
                )
                food.portions.firstOrNull { it.unit == unit }?.let {
                    Text("1 ${Labels.unit(unit)} ≈ ${Labels.grams(it.grams)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (preview != null) {
                    Text(
                        "${Labels.approxKcal(preview.nutrients.energyKcal)} · P ${Labels.grams(preview.nutrients.proteinG)} · C ${Labels.grams(preview.nutrients.carbohydratesG)} · G ${Labels.grams(preview.nutrients.fatG)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text("Fuente: ${Labels.source(food.source)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = { quantity?.let { onConfirm(it, unit) } }, enabled = preview != null, modifier = Modifier.testTag("confirm_quantity")) {
                Text("Listo")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun quickOptions(unit: PortionUnit): List<Double> = when (unit) {
    PortionUnit.GRAM, PortionUnit.MILLILITER -> listOf(50.0, 100.0, 150.0, 200.0)
    PortionUnit.CUP -> listOf(0.5, 1.0, 1.5, 2.0)
    PortionUnit.TABLESPOON, PortionUnit.TEASPOON -> listOf(1.0, 2.0, 3.0)
    else -> listOf(0.5, 1.0, 2.0)
}

/** Alimento propio con valores por 100 g que el usuario conoce (p. ej. de la etiqueta). */
@Composable
fun CustomFoodDialog(initialName: String, onCreate: (String, Nutrients) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    fun num(s: String) = s.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }
    val valid = name.isNotBlank() && num(kcal) != null && num(protein) != null && num(carbs) != null && num(fat) != null &&
        (fiber.isBlank() || num(fiber) != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear alimento") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NoticeCard("Introduce los valores por 100 g, por ejemplo de la etiqueta del producto. La app no inventa estos datos.")
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SmallNumber("kcal", kcal, Modifier.weight(1f)) { kcal = it }
                    SmallNumber("Proteínas g", protein, Modifier.weight(1f)) { protein = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallNumber("Carbohidratos g", carbs, Modifier.weight(1f)) { carbs = it }
                    SmallNumber("Grasas g", fat, Modifier.weight(1f)) { fat = it }
                }
                SmallNumber("Fibra g (opcional)", fiber, Modifier.fillMaxWidth()) { fiber = it }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onCreate(name, Nutrients(num(kcal)!!, num(protein)!!, num(carbs)!!, num(fat)!!, fiber.takeIf { it.isNotBlank() }?.let(::num)))
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun SmallNumber(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { s -> onChange(s.filter { it.isDigit() || it == '.' || it == ',' }.take(6)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
fun SaveBar(enabled: Boolean, pending: Int, onSave: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        if (pending > 0) {
            Text(
                if (pending == 1) "Revisa 1 alimento antes de guardar" else "Revisa $pending alimentos antes de guardar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        PrimaryButton("Guardar comida", onSave, enabled = enabled, modifier = Modifier.testTag("save_meal"))
    }
}
