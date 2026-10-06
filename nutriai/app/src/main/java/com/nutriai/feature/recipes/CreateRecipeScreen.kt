package com.nutriai.feature.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.core.ui.components.GlassChip
import com.nutriai.core.ui.components.GlassIconButton
import com.nutriai.core.ui.components.LargeTitle
import com.nutriai.core.ui.components.NoticeCard
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.core.ui.components.PrimaryButton
import com.nutriai.core.ui.components.SecondaryButton
import com.nutriai.core.ui.components.SectionTitle
import com.nutriai.core.ui.components.ValueRow
import com.nutriai.core.ui.format.Labels
import com.nutriai.domain.model.FoodReference
import com.nutriai.domain.model.FoodTag
import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.model.RecipeComponent
import com.nutriai.domain.nutrition.PortionConverter
import com.nutriai.domain.nutrition.RecipeCalculator
import com.nutriai.domain.repository.FoodRepository
import com.nutriai.domain.repository.RecipeRepository
import com.nutriai.feature.meal.FoodPickerSheet
import com.nutriai.feature.meal.QuantityDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateRecipeState(
    val name: String = "",
    val components: List<RecipeComponent> = emptyList(),
    val tags: Set<FoodTag> = emptySet(),
    val savedId: String? = null,
    val error: String? = null,
) {
    val canSave get() = name.isNotBlank() && components.isNotEmpty()
}

@HiltViewModel
class CreateRecipeViewModel @Inject constructor(
    private val foods: FoodRepository,
    private val recipes: RecipeRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CreateRecipeState())
    val state: StateFlow<CreateRecipeState> = _state.asStateFlow()

    fun setName(n: String) = _state.update { it.copy(name = n.take(50)) }
    fun toggleTag(t: FoodTag) = _state.update { it.copy(tags = if (t in it.tags) it.tags - t else it.tags + t) }
    fun add(food: FoodReference, grams: Double) = _state.update { it.copy(components = it.components + RecipeComponent(food, grams)) }
    fun remove(index: Int) = _state.update { it.copy(components = it.components.filterIndexed { i, _ -> i != index }) }

    suspend fun search(q: String): List<FoodReference> =
        runCatching { foods.search(q) }.getOrDefault(emptyList()).filterNot { it.isRecipe }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (!s.canSave) return@launch
        runCatching { recipes.saveUserRecipe(s.name, s.components, s.tags) }
            .onSuccess { id -> _state.update { it.copy(savedId = id) } }
            .onFailure { _state.update { it.copy(error = "No pudimos guardar la receta. Inténtalo de nuevo.") } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateRecipeScreen(onBack: () -> Unit, onSaved: (String) -> Unit, viewModel: CreateRecipeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedId) { state.savedId?.let(onSaved) }
    var picking by remember { mutableStateOf(false) }
    var quantityFor by remember { mutableStateOf<FoodReference?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            GlassIconButton(onClick = onBack, contentDescription = "Volver") { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
            LargeTitle("Nueva receta")
        }
        item {
            OutlinedTextField(
                value = state.name, onValueChange = viewModel::setName, label = { Text("Nombre de la receta") },
                singleLine = true, shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { SectionTitle("Ingredientes (para una porción)") }
        itemsIndexed(state.components) { i, c ->
            NutriCard(contentPadding = PaddingValues(start = 18.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.food.name, modifier = Modifier.weight(1f))
                    Text(Labels.grams(c.baseAmount), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = { viewModel.remove(i) }) { Icon(Icons.Rounded.Delete, "Quitar") }
                }
            }
        }
        item { SecondaryButton("+ Añadir ingrediente", { picking = true }) }
        if (state.components.isNotEmpty()) {
            item {
                val total = RecipeCalculator.totalNutrients(state.components)
                NutriCard {
                    Text("Total por porción", style = MaterialTheme.typography.titleMedium)
                    ValueRow("Calorías", Labels.approxKcal(total.energyKcal))
                    ValueRow("Proteínas", Labels.grams(total.proteinG))
                    ValueRow("Carbohidratos", Labels.grams(total.carbohydratesG))
                    ValueRow("Grasas", Labels.grams(total.fatG))
                }
            }
        }
        item {
            SectionTitle("Etiquetas")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FoodTag.entries.filter { it != FoodTag.COSTA_RICA }.forEach { t -> GlassChip(Labels.tag(t), t in state.tags, { viewModel.toggleTag(t) }) }
            }
        }
        state.error?.let { item { NoticeCard(it, isWarning = true) } }
        item {
            Spacer(Modifier.height(4.dp))
            PrimaryButton("Guardar receta", viewModel::save, enabled = state.canSave)
        }
    }

    if (picking) {
        FoodPickerSheet(
            initialQuery = "",
            search = viewModel::search,
            onPicked = { food -> picking = false; quantityFor = food },
            onCreateCustom = { picking = false },
            onDismiss = { picking = false },
        )
    }
    quantityFor?.let { food ->
        QuantityDialog(
            food = food, initialQuantity = 100.0, initialUnit = PortionUnit.GRAM,
            onConfirm = { q, u ->
                PortionConverter.toBaseAmount(q, u, food)?.let { viewModel.add(food, it) }
                quantityFor = null
            },
            onDismiss = { quantityFor = null },
        )
    }
}
