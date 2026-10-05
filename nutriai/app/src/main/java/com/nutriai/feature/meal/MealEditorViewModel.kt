package com.nutriai.feature.meal

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.nutriai.data.image.ImageProcessor
import com.nutriai.data.repository.Clock
import com.nutriai.data.settings.SettingsRepository
import com.nutriai.core.ui.format.Labels
import com.nutriai.domain.ai.FoodAnalysisService
import com.nutriai.domain.model.FoodCategory
import com.nutriai.domain.model.FoodReference
import com.nutriai.domain.model.MealOrigin
import com.nutriai.domain.model.MealType
import com.nutriai.domain.model.NutrientBasis
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.model.NutritionSource
import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.repository.FoodRepository
import com.nutriai.domain.repository.MealRepository
import com.nutriai.domain.result.AppError
import com.nutriai.domain.result.AppResult
import com.nutriai.navigation.MealEditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EditorMode { NEW, EDIT, ANALYSIS }

sealed interface EditorPhase {
    data object Loading : EditorPhase
    data object Analyzing : EditorPhase
    data class Failed(val error: AppError) : EditorPhase
    data object Editing : EditorPhase
    data class Saved(val kcal: Int) : EditorPhase
}

data class MealEditorState(
    val mode: EditorMode,
    val phase: EditorPhase = EditorPhase.Loading,
    val draft: MealDraft? = null,
    val providerName: String? = null,
    val isMockAnalysis: Boolean = false,
    val saveError: String? = null,
    val deleted: Boolean = false,
)

@HiltViewModel
class MealEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val meals: MealRepository,
    private val foods: FoodRepository,
    private val analysis: FoodAnalysisService,
    private val images: ImageProcessor,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<MealEditorRoute>()
    private val mode = when {
        route.imageUri.isNotBlank() -> EditorMode.ANALYSIS
        route.mealId >= 0 -> EditorMode.EDIT
        else -> EditorMode.NEW
    }
    private var keyCounter = 0L
    private fun nextKey() = ++keyCounter

    private val _state = MutableStateFlow(MealEditorState(mode))
    val state: StateFlow<MealEditorState> = _state.asStateFlow()

    private val initialType = runCatching { MealType.valueOf(route.mealType) }.getOrNull() ?: Labels.defaultMealType()
    private val initialDate = if (route.epochDay == Long.MIN_VALUE) clock.today() else LocalDate.ofEpochDay(route.epochDay)

    init {
        when (mode) {
            EditorMode.NEW -> _state.update {
                it.copy(phase = EditorPhase.Editing, draft = MealDraft(type = initialType, date = initialDate, origin = MealOrigin.MANUAL))
            }
            EditorMode.EDIT -> loadExisting(route.mealId)
            EditorMode.ANALYSIS -> analyze()
        }
    }

    private fun loadExisting(id: Long) = viewModelScope.launch {
        val meal = runCatching { meals.getMeal(id) }.getOrNull()
        if (meal == null) {
            _state.update { it.copy(phase = EditorPhase.Failed(AppError.Storage)) }
            return@launch
        }
        val catalog = foods.allFoods().associateBy { it.id }
        val items = meal.items.map { item ->
            val food = item.foodId?.let(catalog::get)
            if (food != null) {
                DraftItem(nextKey(), food, item.quantity, item.unit, aiConfidence = item.aiConfidence)
            } else {
                DraftItem(nextKey(), null, item.quantity, item.unit, frozen = item)
            }
        }
        _state.update {
            it.copy(
                phase = EditorPhase.Editing,
                draft = MealDraft(meal.id, meal.type, meal.date, meal.origin, items, meal.analysisConfidence, meal.createdAtEpochMs),
            )
        }
    }

    fun analyze() = viewModelScope.launch {
        _state.update { it.copy(phase = EditorPhase.Analyzing, providerName = analysis.providerName, isMockAnalysis = analysis.isMock) }
        val image = withContext(Dispatchers.IO) { images.load(Uri.parse(route.imageUri)) }
        if (image == null) {
            _state.update { it.copy(phase = EditorPhase.Failed(AppError.UnreadableImage)) }
            cleanupPhoto()
            return@launch
        }
        val result = runCatching { analysis.analyze(image) }.getOrElse { AppResult.Failure(AppError.Unexpected) }
        // La foto ya no se necesita: no se guarda de forma permanente.
        cleanupPhoto()
        when (result) {
            is AppResult.Failure -> _state.update { it.copy(phase = EditorPhase.Failed(result.error)) }
            is AppResult.Success -> {
                val catalog = foods.allFoods()
                val items = MealDraft.fromDetected(result.value.items, catalog, ::nextKey)
                _state.update {
                    it.copy(
                        phase = EditorPhase.Editing,
                        isMockAnalysis = result.value.isMock,
                        providerName = result.value.providerName,
                        draft = MealDraft(
                            type = initialType, date = initialDate, origin = MealOrigin.AI_PHOTO,
                            items = items, analysisConfidence = result.value.overallConfidence,
                        ),
                    )
                }
            }
        }
    }

    private suspend fun cleanupPhoto() {
        if (!route.deleteImageAfter) return
        withContext(Dispatchers.IO) {
            Uri.parse(route.imageUri).path?.let { File(it).delete() }
        }
    }

    /** Pasa a registro manual tras un error de análisis. */
    fun switchToManual() = _state.update {
        it.copy(phase = EditorPhase.Editing, draft = MealDraft(type = initialType, date = initialDate, origin = MealOrigin.MANUAL))
    }

    private fun updateDraft(transform: (MealDraft) -> MealDraft) =
        _state.update { s -> s.copy(draft = s.draft?.let(transform), saveError = null) }

    fun setType(type: MealType) = updateDraft { it.copy(type = type) }

    fun addItem(food: FoodReference, quantity: Double, unit: PortionUnit) =
        updateDraft { it.copy(items = it.items + DraftItem(nextKey(), food, quantity, unit)) }

    fun replaceItem(key: Long, food: FoodReference, quantity: Double, unit: PortionUnit) = updateDraft { draft ->
        draft.copy(items = draft.items.map { if (it.key == key) it.copy(food = food, quantity = quantity, unit = unit, needsReview = false, frozen = null) else it })
    }

    fun confirmItem(key: Long) = updateDraft { draft ->
        draft.copy(items = draft.items.map { if (it.key == key && it.mealItem != null) it.copy(needsReview = false) else it })
    }

    fun removeItem(key: Long) = updateDraft { draft -> draft.copy(items = draft.items.filterNot { it.key == key }) }

    fun expandRecipe(key: Long) = updateDraft { draft ->
        val index = draft.items.indexOfFirst { it.key == key }
        val parts = draft.items.getOrNull(index)?.let { MealDraft.expandRecipe(it, ::nextKey) } ?: return@updateDraft draft
        draft.copy(items = draft.items.toMutableList().apply { removeAt(index); addAll(index, parts) })
    }

    suspend fun searchFoods(query: String): List<FoodReference> = runCatching { foods.search(query) }.getOrDefault(emptyList())

    /** Crea un alimento con valores que el usuario conoce (p. ej. de una etiqueta). */
    suspend fun createCustomFood(name: String, per100: Nutrients): FoodReference? = runCatching {
        val food = FoodReference(
            id = "", name = name.trim(), nutrientsPerBasis = per100, basis = NutrientBasis.PER_100_G,
            source = NutritionSource.USER_ENTERED, sourceReference = "Valores introducidos por el usuario",
            category = FoodCategory.OTHER,
        )
        val id = foods.saveCustomFood(food)
        foods.getFood(id)
    }.getOrNull()

    fun save() = viewModelScope.launch {
        val draft = _state.value.draft ?: return@launch
        val profileId = settings.current().activeProfileId ?: return@launch
        val meal = draft.toMeal(profileId, clock.nowEpochMs()) ?: return@launch
        runCatching { meals.save(meal) }
            .onSuccess { _state.update { it.copy(phase = EditorPhase.Saved(meal.nutrients.energyKcal.toInt())) } }
            .onFailure { _state.update { it.copy(saveError = Labels.error(AppError.Storage)) } }
    }

    fun deleteMeal() = viewModelScope.launch {
        val id = _state.value.draft?.mealId?.takeIf { it > 0 } ?: return@launch
        runCatching { meals.delete(id) }
            .onSuccess { _state.update { it.copy(deleted = true) } }
            .onFailure { _state.update { it.copy(saveError = Labels.error(AppError.Storage)) } }
    }
}
