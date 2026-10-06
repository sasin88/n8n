package com.nutriai.feature.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutriai.core.ui.components.AppearIn
import com.nutriai.core.ui.components.EmptyState
import com.nutriai.core.ui.components.GlassChip
import com.nutriai.core.ui.components.GlassIconButton
import com.nutriai.core.ui.components.LargeTitle
import com.nutriai.core.ui.components.NoticeCard
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.core.ui.components.SectionTitle
import com.nutriai.core.ui.format.Labels
import com.nutriai.core.ui.theme.CarbsColor
import com.nutriai.core.ui.theme.FatColor
import com.nutriai.core.ui.theme.ProteinColor
import com.nutriai.domain.model.FoodReference
import com.nutriai.domain.model.FoodTag
import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.nutrition.CalorieRange
import com.nutriai.domain.nutrition.RecipeFilters
import kotlin.math.roundToInt

fun recipeEmoji(r: FoodReference): String = when {
    FoodTag.COSTA_RICA in r.tags -> "🇨🇷"
    FoodTag.BREAKFAST in r.tags -> "🥣"
    FoodTag.VEGAN in r.tags -> "🥗"
    FoodTag.HIGH_PROTEIN in r.tags -> "🍗"
    FoodTag.SNACK in r.tags -> "🍓"
    r.isUserCreated -> "👩‍🍳"
    else -> "🍽️"
}

@Composable
fun RecipesScreen(
    onOpenRecipe: (String) -> Unit,
    onCreateRecipe: () -> Unit,
    viewModel: RecipesViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.reload() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            LargeTitle("Recetas", subtitle = "${state.all.size} recetas") {
                GlassIconButton(onClick = onCreateRecipe, contentDescription = "Crear receta", modifier = Modifier.testTag("create_recipe")) {
                    Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                placeholder = { Text("Buscar recetas") },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                    unfocusedBorderColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Column {
                SectionTitle("Por calorías (por porción)")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CalorieRange.entries) { r -> GlassChip(Labels.calorieRange(r), state.range == r, { viewModel.setRange(r) }) }
                }
                Spacer(Modifier.height(8.dp))
                SectionTitle("Por estilo de alimentación")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FoodTag.entries) { t -> GlassChip(Labels.tag(t), t in state.tags, { viewModel.toggleTag(t) }) }
                }
            }
        }
        if (state.isFiltering) {
            val results = state.filtered
            if (results.isEmpty()) {
                item { EmptyState("🔎", "Sin resultados", "Prueba con otros filtros o crea tu propia receta.") }
            } else {
                items(results, key = { it.id }) { r -> RecipeRow(r) { onOpenRecipe(r.id) } }
            }
        } else {
            if (state.mine.isNotEmpty()) item { Collection("Mis recetas", state.mine, onOpenRecipe) }
            item { Collection("Platos de Costa Rica", state.collection(FoodTag.COSTA_RICA), onOpenRecipe) }
            item { Collection("Come como un campeón · Alta proteína", state.collection(FoodTag.HIGH_PROTEIN), onOpenRecipe) }
            item { Collection("Desayunos", state.collection(FoodTag.BREAKFAST), onOpenRecipe) }
            item { Collection("Bajo en carbohidratos", state.collection(FoodTag.LOW_CARB), onOpenRecipe) }
            item { Collection("Vegano y vegetariano", state.all.filter { FoodTag.VEGETARIAN in it.tags || FoodTag.VEGAN in it.tags }, onOpenRecipe) }
            item { Collection("Rápidas", state.collection(FoodTag.QUICK), onOpenRecipe) }
            item {
                NoticeCard("Recetas con composición aproximada calculada por NutriAI con ingredientes de USDA. Ajusta las cantidades al registrar.")
            }
        }
    }
}

@Composable
private fun Collection(title: String, recipes: List<FoodReference>, onOpen: (String) -> Unit) {
    if (recipes.isEmpty()) return
    Column {
        SectionTitle(title)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(recipes, key = { it.id }) { r ->
                AppearIn {
                    NutriCard(Modifier.width(190.dp), onClick = { onOpen(r.id) }, contentPadding = PaddingValues(16.dp)) {
                        Text(recipeEmoji(r), style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(10.dp))
                        Text(r.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(44.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("≈ ${RecipeFilters.kcalPerServing(r).roundToInt()} kcal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeRow(r: FoodReference, onClick: () -> Unit) {
    val serving = r.portions.firstOrNull { it.unit == PortionUnit.SERVING }?.grams ?: 100.0
    val n = r.nutrientsPerBasis.scaledBy(serving / 100)
    NutriCard(onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(recipeEmoji(r), style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(r.name, style = MaterialTheme.typography.titleMedium)
                Row {
                    Text("P ${n.proteinG.roundToInt()}g  ", style = MaterialTheme.typography.labelMedium, color = ProteinColor)
                    Text("C ${n.carbohydratesG.roundToInt()}g  ", style = MaterialTheme.typography.labelMedium, color = CarbsColor)
                    Text("G ${n.fatG.roundToInt()}g", style = MaterialTheme.typography.labelMedium, color = FatColor)
                }
            }
            Text("≈ ${n.energyKcal.roundToInt()}\nkcal", style = MaterialTheme.typography.labelLarge)
        }
    }
}
