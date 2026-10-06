package com.nutriai.feature.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.nutriai.core.ui.components.ConfirmDialog
import com.nutriai.core.ui.components.DangerButton
import com.nutriai.core.ui.components.EstimateNote
import com.nutriai.core.ui.components.GlassChip
import com.nutriai.core.ui.components.GlassIconButton
import com.nutriai.core.ui.components.LargeTitle
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.core.ui.components.PrimaryButton
import com.nutriai.core.ui.components.ValueRow
import com.nutriai.core.ui.format.Labels
import com.nutriai.domain.model.FoodReference
import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.repository.FoodRepository
import com.nutriai.domain.repository.RecipeRepository
import com.nutriai.navigation.RecipeDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foods: FoodRepository,
    private val recipes: RecipeRepository,
) : ViewModel() {
    val id = savedStateHandle.toRoute<RecipeDetailRoute>().recipeId
    private val _recipe = MutableStateFlow<FoodReference?>(null)
    val recipe: StateFlow<FoodReference?> = _recipe.asStateFlow()
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    init { viewModelScope.launch { _recipe.value = runCatching { foods.getFood(id) }.getOrNull() } }

    fun delete() = viewModelScope.launch { runCatching { recipes.deleteUserRecipe(id) }.onSuccess { _deleted.value = true } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailScreen(onBack: () -> Unit, onLog: (String) -> Unit, viewModel: RecipeDetailViewModel = hiltViewModel()) {
    val recipe by viewModel.recipe.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(deleted) { if (deleted) onBack() }
    var confirmDelete by remember { mutableStateOf(false) }
    val r = recipe
    if (r == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val serving = r.portions.firstOrNull { it.unit == PortionUnit.SERVING }?.grams ?: 100.0
    val n = r.nutrientsPerBasis.scaledBy(serving / 100)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            GlassIconButton(onClick = onBack, contentDescription = "Volver") { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
            Spacer(Modifier.height(12.dp))
            Text(recipeEmoji(r), style = MaterialTheme.typography.displayLarge)
            LargeTitle(r.name)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                r.tags.forEach { GlassChip(Labels.tag(it), false, {}) }
            }
        }
        item {
            NutriCard {
                Text("POR PORCIÓN (${Labels.grams(serving)})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Labels.approxKcal(n.energyKcal), style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(8.dp))
                ValueRow("Proteínas", Labels.grams(n.proteinG))
                ValueRow("Carbohidratos", Labels.grams(n.carbohydratesG))
                ValueRow("Grasas", Labels.grams(n.fatG))
                ValueRow("Fibra", n.fiberG?.let(Labels::grams) ?: "sin datos completos")
            }
        }
        item {
            NutriCard {
                Text("Ingredientes", style = MaterialTheme.typography.titleMedium)
                r.components.forEach { c ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text("• ${c.food.name}", modifier = Modifier.weight(1f))
                        Text(Labels.grams(c.baseAmount), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            PrimaryButton("Registrar en una comida", { onLog(r.id) })
            Spacer(Modifier.height(8.dp))
            EstimateNote(r.sourceReference ?: "Valores estimados.")
        }
        if (r.isUserCreated) {
            item { DangerButton("Eliminar receta", { confirmDelete = true }) }
        }
    }
    if (confirmDelete) {
        ConfirmDialog("¿Eliminar receta?", "Las comidas ya registradas no cambian.", "Eliminar",
            onConfirm = { confirmDelete = false; viewModel.delete() }, onDismiss = { confirmDelete = false }, destructive = true)
    }
}
