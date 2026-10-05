package com.nutriai.feature.meal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutriai.core.ui.components.ConfidenceBadge
import com.nutriai.core.ui.components.ConfirmDialog
import com.nutriai.core.ui.components.EstimateNote
import com.nutriai.core.ui.components.NoticeCard
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.core.ui.components.PrimaryButton
import com.nutriai.core.ui.components.SecondaryButton
import com.nutriai.core.ui.format.Labels
import com.nutriai.domain.model.FoodReference
import com.nutriai.domain.model.MealType
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.model.PortionUnit
import kotlinx.coroutines.launch

/** Estado de los diálogos del editor. */
private sealed interface EditorDialog {
    data class Search(val replacingKey: Long?, val query: String) : EditorDialog
    data class Quantity(val food: FoodReference, val replacingKey: Long?, val item: DraftItem?) : EditorDialog
    data class CreateFood(val replacingKey: Long?, val name: String) : EditorDialog
    data object DeleteMeal : EditorDialog
}

@Composable
fun MealEditorScreen(
    onBack: () -> Unit,
    onSaved: (Int) -> Unit,
    onDeleted: () -> Unit,
    viewModel: MealEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.phase) { (state.phase as? EditorPhase.Saved)?.let { onSaved(it.kcal) } }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }
    val scope = rememberCoroutineScope()

    MealEditorContent(
        state = state,
        onBack = onBack,
        onManual = viewModel::switchToManual,
        onTypeChange = viewModel::setType,
        onAdd = viewModel::addItem,
        onReplace = viewModel::replaceItem,
        onConfirm = viewModel::confirmItem,
        onRemove = viewModel::removeItem,
        onExpand = viewModel::expandRecipe,
        onSave = viewModel::save,
        onDelete = viewModel::deleteMeal,
        search = viewModel::searchFoods,
        createFood = { name, nutrients, then -> scope.launch { viewModel.createCustomFood(name, nutrients)?.let(then) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealEditorContent(
    state: MealEditorState,
    onBack: () -> Unit,
    onManual: () -> Unit,
    onTypeChange: (MealType) -> Unit,
    onAdd: (FoodReference, Double, PortionUnit) -> Unit,
    onReplace: (Long, FoodReference, Double, PortionUnit) -> Unit,
    onConfirm: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onExpand: (Long) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    search: suspend (String) -> List<FoodReference>,
    createFood: (String, Nutrients, (FoodReference) -> Unit) -> Unit,
) {
    var dialog by remember { mutableStateOf<EditorDialog?>(null) }
    val title = when (state.mode) {
        EditorMode.NEW -> "Registrar comida"
        EditorMode.EDIT -> "Editar comida"
        EditorMode.ANALYSIS -> if (state.phase is EditorPhase.Editing) "Resultado del análisis" else "Analizar comida"
    }
    val draft = state.draft

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                actions = {
                    if (state.mode == EditorMode.EDIT && draft != null) {
                        IconButton(onClick = { dialog = EditorDialog.DeleteMeal }) { Icon(Icons.Default.Delete, "Eliminar comida") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (state.phase is EditorPhase.Editing && draft != null) {
                SaveBar(enabled = draft.canSave, pending = draft.pendingCount, onSave = onSave)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        AnimatedContent(
            targetState = state.phase::class,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
            modifier = Modifier.padding(padding),
            label = "phase",
        ) { phaseClass ->
            when {
                phaseClass == EditorPhase.Analyzing::class -> AnalyzingView(state.isMockAnalysis)
                phaseClass == EditorPhase.Failed::class -> {
                    val error = (state.phase as? EditorPhase.Failed)?.error
                    FailedView(error?.let(Labels::error) ?: "", onManual, onBack)
                }
                phaseClass == EditorPhase.Editing::class && draft != null -> EditorBody(
                    state = state,
                    draft = draft,
                    onTypeChange = onTypeChange,
                    onOpenItem = { item ->
                        dialog = if (item.food != null) EditorDialog.Quantity(item.food, item.key, item)
                        else EditorDialog.Search(item.key, item.detectedName.orEmpty())
                    },
                    onChangeFood = { item -> dialog = EditorDialog.Search(item.key, item.detectedName ?: item.food?.name.orEmpty()) },
                    onConfirm = onConfirm,
                    onRemove = onRemove,
                    onExpand = onExpand,
                    onAddFood = { dialog = EditorDialog.Search(null, "") },
                )
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }

    when (val d = dialog) {
        is EditorDialog.Search -> FoodPickerSheet(
            initialQuery = d.query,
            search = search,
            onPicked = { food -> dialog = EditorDialog.Quantity(food, d.replacingKey, null) },
            onCreateCustom = { name -> dialog = EditorDialog.CreateFood(d.replacingKey, name) },
            onDismiss = { dialog = null },
        )
        is EditorDialog.Quantity -> QuantityDialog(
            food = d.food,
            initialQuantity = d.item?.quantity,
            initialUnit = d.item?.unit,
            onConfirm = { quantity, unit ->
                if (d.replacingKey != null) onReplace(d.replacingKey, d.food, quantity, unit) else onAdd(d.food, quantity, unit)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is EditorDialog.CreateFood -> CustomFoodDialog(
            initialName = d.name,
            onCreate = { name, nutrients -> createFood(name, nutrients) { food -> dialog = EditorDialog.Quantity(food, d.replacingKey, null) } },
            onDismiss = { dialog = null },
        )
        EditorDialog.DeleteMeal -> ConfirmDialog(
            title = "¿Eliminar esta comida?",
            message = "Se quitará de tu historial. Esta acción no se puede deshacer.",
            confirmText = "Eliminar",
            destructive = true,
            onConfirm = { dialog = null; onDelete() },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun EditorBody(
    state: MealEditorState,
    draft: MealDraft,
    onTypeChange: (MealType) -> Unit,
    onOpenItem: (DraftItem) -> Unit,
    onChangeFood: (DraftItem) -> Unit,
    onConfirm: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onExpand: (Long) -> Unit,
    onAddFood: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.mode == EditorMode.ANALYSIS || draft.analysisConfidence != null) {
            item {
                if (state.isMockAnalysis) {
                    NoticeCard(
                        title = "Modo demostración",
                        text = "Este resultado es SIMULADO: la IA real aún no está conectada y la foto no se analizó. Sirve para probar el flujo.",
                        isWarning = true,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                draft.analysisConfidence?.let { confidence ->
                    NutriCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Confianza del análisis", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            ConfidenceBadge(confidence)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Revisa los alimentos y las cantidades. Las calorías de una foto son siempre una estimación.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MealType.entries) { type ->
                    FilterChip(
                        selected = draft.type == type,
                        onClick = { onTypeChange(type) },
                        label = { Text("${Labels.mealEmoji(type)} ${Labels.mealType(type)}") },
                    )
                }
            }
        }
        if (draft.items.isEmpty()) {
            item {
                Text(
                    "Añade los alimentos que comiste. Puedes buscar platos típicos como gallo pinto o casado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(draft.items, key = { it.key }) { item ->
            DraftItemCard(
                item = item,
                onOpen = { onOpenItem(item) },
                onChangeFood = { onChangeFood(item) },
                onConfirm = { onConfirm(item.key) },
                onRemove = { onRemove(item.key) },
                onExpand = { onExpand(item.key) },
            )
        }
        item { SecondaryButton("+ Añadir alimento", onAddFood, modifier = Modifier.testTag("add_food")) }
        if (draft.items.isNotEmpty()) {
            item { TotalCard(draft.total) }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun DraftItemCard(
    item: DraftItem,
    onOpen: () -> Unit,
    onChangeFood: () -> Unit,
    onConfirm: () -> Unit,
    onRemove: () -> Unit,
    onExpand: () -> Unit,
) {
    val meal = item.mealItem
    NutriCard(onClick = onOpen, contentPadding = PaddingValues(start = 18.dp, end = 6.dp, top = 14.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(item.displayName, style = MaterialTheme.typography.titleMedium)
                if (item.detectedName != null && item.food != null && item.detectedName != item.food.name) {
                    Text("La IA vio: ${item.detectedName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        meal != null -> "${Labels.quantity(meal.quantity, meal.unit)} · ${Labels.approxKcal(meal.nutrients.energyKcal)}"
                        item.food == null -> "Elige el alimento correcto"
                        else -> "Indica la cantidad"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (meal != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                )
            }
            item.aiConfidence?.let { ConfidenceBadge(it, Modifier.padding(top = 2.dp)) }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "Quitar ${item.displayName}") }
        }
        if (item.needsReview) {
            Spacer(Modifier.height(8.dp))
            NoticeCard(
                if (item.food == null) "No estoy completamente seguro de este alimento y no lo encontré en la base de datos."
                else "No estoy completamente seguro de este alimento. ¿Es correcto?",
                isWarning = true,
            )
            Row {
                TextButton(onClick = onChangeFood) { Text("Elegir otro") }
                if (meal != null) TextButton(onClick = onConfirm, modifier = Modifier.testTag("confirm_item")) { Text("Sí, es correcto") }
            }
        } else {
            Row {
                TextButton(onClick = onOpen) { Text("Cambiar cantidad") }
                if (item.food?.isRecipe == true && meal != null) TextButton(onClick = onExpand) { Text("Ver ingredientes") }
            }
        }
    }
}

@Composable
private fun TotalCard(total: Nutrients) {
    NutriCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TOTAL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(Labels.approxKcal(total.energyKcal), style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("total_kcal"))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Macro("Proteínas", total.proteinG)
            Macro("Carbohidratos", total.carbohydratesG)
            Macro("Grasas", total.fatG)
            Macro("Fibra", total.fiberG)
        }
        Spacer(Modifier.height(12.dp))
        EstimateNote()
    }
}

@Composable
private fun Macro(label: String, grams: Double?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(grams?.let { Labels.grams(it) } ?: "sin datos", style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Animación discreta mientras se analiza la foto. */
@Composable
private fun AnalyzingView(isMock: Boolean) {
    val transition = rememberInfiniteTransition(label = "analyzing")
    val scale by transition.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🍽️", style = MaterialTheme.typography.displaySmall, modifier = Modifier.scale(scale))
        Spacer(Modifier.height(16.dp))
        Text("Analizando…", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            if (isMock) "Modo demostración: el resultado será simulado." else "Identificando alimentos y estimando porciones.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator()
    }
}

@Composable
private fun FailedView(message: String, onManual: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("😕", style = MaterialTheme.typography.displaySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Registrar manualmente", onManual)
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Probar con otra foto", onBack)
    }
}
