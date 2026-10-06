package com.nutriai.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriai.data.repository.Clock
import com.nutriai.domain.activity.ExerciseCalculator
import com.nutriai.domain.activity.ExerciseType
import com.nutriai.domain.activity.FastingSession
import com.nutriai.domain.health.Hydration
import com.nutriai.domain.model.ExerciseLog
import com.nutriai.domain.model.Meal
import com.nutriai.domain.model.MealType
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.model.WaterLog
import com.nutriai.domain.model.WeightRecord
import com.nutriai.domain.repository.ExerciseRepository
import com.nutriai.domain.repository.FastingRepository
import com.nutriai.domain.repository.MealRepository
import com.nutriai.domain.repository.WaterRepository
import com.nutriai.domain.repository.WeightRepository
import com.nutriai.feature.common.ActiveProfileTargets
import com.nutriai.feature.common.ProfileWithTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardState(
    val loading: Boolean = true,
    val data: ProfileWithTarget? = null,
    val today: LocalDate = LocalDate.now(),
    val consumed: Nutrients = Nutrients.ZERO,
    val meals: Map<MealType, List<Meal>> = emptyMap(),
    val waterMl: Int = 0,
    val exercise: List<ExerciseLog> = emptyList(),
    val activeFast: FastingSession? = null,
    val lastWeight: WeightRecord? = null,
) {
    val burnedKcal: Int get() = exercise.sumOf { it.kcal }
    /** Presupuesto del día: objetivo + ejercicio si el usuario lo eligió. */
    val budgetKcal: Int get() = (data?.target?.effectiveKcal ?: 0) + if (data?.profile?.exerciseAddsToBudget == true) burnedKcal else 0
    val waterGoalMl: Int get() = data?.profile?.let { it.waterGoalMl ?: Hydration.suggestedGoalMl(it.weightKg) } ?: 2000
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    targets: ActiveProfileTargets,
    private val meals: MealRepository,
    private val water: WaterRepository,
    private val exercise: ExerciseRepository,
    private val fasting: FastingRepository,
    private val weights: WeightRepository,
    private val clock: Clock,
) : ViewModel() {
    private val today = MutableStateFlow(clock.today())

    val state: StateFlow<DashboardState> = combine(targets.flow, today) { d, t -> d to t }.flatMapLatest { (data, day) ->
        if (data == null) {
            flowOf(DashboardState(loading = false))
        } else {
            val id = data.profile.id
            combine(
                meals.observeMeals(id, day, day),
                water.observe(id, day, day),
                exercise.observe(id, day, day),
                fasting.observeActive(id),
                weights.observeRecords(id),
            ) { mealList, waterList: List<WaterLog>, exerciseList, fast, weightList ->
                DashboardState(
                    loading = false,
                    data = data,
                    today = day,
                    consumed = Nutrients.sum(mealList.map { it.nutrients }),
                    meals = mealList.groupBy { it.type },
                    waterMl = waterList.sumOf { it.ml },
                    exercise = exerciseList,
                    activeFast = fast,
                    lastWeight = weightList.lastOrNull(),
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    fun refreshDay() { today.value = clock.today() }

    fun addWater(ml: Int = Hydration.CUP_ML) = viewModelScope.launch {
        val p = state.value.data?.profile ?: return@launch
        runCatching { water.add(WaterLog(profileId = p.id, date = clock.today(), ml = ml, createdAtEpochMs = clock.nowEpochMs())) }
    }

    fun removeWater() = viewModelScope.launch {
        val p = state.value.data?.profile ?: return@launch
        runCatching { water.removeLast(p.id, clock.today()) }
    }

    /** kcal = null → se estima con MET. */
    fun addExercise(type: ExerciseType, minutes: Int, kcal: Int?) = viewModelScope.launch {
        val p = state.value.data?.profile ?: return@launch
        val estimated = kcal == null
        val value = kcal ?: ExerciseCalculator.estimateKcal(type, minutes, p.weightKg) ?: return@launch
        runCatching { exercise.add(ExerciseLog(profileId = p.id, date = clock.today(), type = type, minutes = minutes, kcal = value, estimated = estimated)) }
    }

    fun deleteExercise(id: Long) = viewModelScope.launch { runCatching { exercise.delete(id) } }
}
