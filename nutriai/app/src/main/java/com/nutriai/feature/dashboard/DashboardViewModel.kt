package com.nutriai.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriai.data.repository.Clock
import com.nutriai.domain.model.Meal
import com.nutriai.domain.model.MealType
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.repository.MealRepository
import com.nutriai.feature.common.ActiveProfileTargets
import com.nutriai.feature.common.ProfileWithTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DashboardState(
    val loading: Boolean = true,
    val data: ProfileWithTarget? = null,
    val today: LocalDate = LocalDate.now(),
    val consumed: Nutrients = Nutrients.ZERO,
    val meals: Map<MealType, List<Meal>> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    targets: ActiveProfileTargets,
    private val meals: MealRepository,
    private val clock: Clock,
) : ViewModel() {
    private val today = MutableStateFlow(clock.today())

    val state: StateFlow<DashboardState> = targets.flow.flatMapLatest { data ->
        if (data == null) {
            flowOf(DashboardState(loading = false))
        } else {
            today.flatMapLatest { day ->
                meals.observeMeals(data.profile.id, day, day).map { list ->
                    DashboardState(
                        loading = false,
                        data = data,
                        today = day,
                        consumed = Nutrients.sum(list.map { it.nutrients }),
                        meals = list.groupBy { it.type },
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    /** Se llama al volver a la pantalla, por si cambió el día. */
    fun refreshDay() {
        today.value = clock.today()
    }
}
