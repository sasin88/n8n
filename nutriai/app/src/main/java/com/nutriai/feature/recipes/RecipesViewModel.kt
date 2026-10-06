package com.nutriai.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriai.domain.model.FoodReference
import com.nutriai.domain.model.FoodTag
import com.nutriai.domain.nutrition.CalorieRange
import com.nutriai.domain.nutrition.RecipeFilters
import com.nutriai.domain.repository.FoodRepository
import com.nutriai.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipesState(
    val all: List<FoodReference> = emptyList(),
    val query: String = "",
    val range: CalorieRange? = null,
    val tags: Set<FoodTag> = emptySet(),
    val loading: Boolean = true,
) {
    val filtered: List<FoodReference> get() = RecipeFilters.filter(all, range, tags, query)
    val isFiltering: Boolean get() = query.isNotBlank() || range != null || tags.isNotEmpty()
    fun collection(tag: FoodTag) = all.filter { tag in it.tags }
    val mine: List<FoodReference> get() = all.filter { it.isUserCreated }
}

@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val foods: FoodRepository,
    profiles: ProfileRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RecipesState())
    val state: StateFlow<RecipesState> = _state.asStateFlow()

    init {
        // Recargar al cambiar de perfil (las recetas propias son por perfil).
        viewModelScope.launch {
            profiles.observeActiveProfile().map { it?.id }.distinctUntilChanged().collect { reload() }
        }
    }

    fun reload() = viewModelScope.launch {
        val recipes = runCatching { foods.allFoods() }.getOrDefault(emptyList()).filter { it.isRecipe }
        _state.update { it.copy(all = recipes, loading = false) }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun setRange(r: CalorieRange?) = _state.update { it.copy(range = if (it.range == r) null else r) }
    fun toggleTag(t: FoodTag) = _state.update { it.copy(tags = if (t in it.tags) it.tags - t else it.tags + t) }
}
