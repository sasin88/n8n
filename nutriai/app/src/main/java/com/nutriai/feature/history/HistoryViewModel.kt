package com.nutriai.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriai.data.repository.Clock
import com.nutriai.domain.model.DailySummary
import com.nutriai.domain.model.Meal
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.model.WeightRecord
import com.nutriai.domain.nutrition.NutritionStats
import com.nutriai.domain.repository.MealRepository
import com.nutriai.domain.repository.WeightRepository
import com.nutriai.feature.common.ActiveProfileTargets
import com.nutriai.feature.common.ProfileWithTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class HistoryPeriod { DAY, WEEK, MONTH }

data class HistoryState(
    val period: HistoryPeriod = HistoryPeriod.DAY,
    val anchor: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val from: LocalDate = LocalDate.now(),
    val to: LocalDate = LocalDate.now(),
    val data: ProfileWithTarget? = null,
    val meals: List<Meal> = emptyList(),
    val summaries: List<DailySummary> = emptyList(),
    val average: Nutrients? = null,
    val weights: List<WeightRecord> = emptyList(),
) {
    val canGoForward: Boolean get() = to.isBefore(today)
}

/** Rango del periodo que contiene [anchor]. Las semanas empiezan en lunes. */
fun periodRange(period: HistoryPeriod, anchor: LocalDate): Pair<LocalDate, LocalDate> = when (period) {
    HistoryPeriod.DAY -> anchor to anchor
    HistoryPeriod.WEEK -> {
        val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        start to start.plusDays(6)
    }
    HistoryPeriod.MONTH -> anchor.withDayOfMonth(1) to anchor.with(TemporalAdjusters.lastDayOfMonth())
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    targets: ActiveProfileTargets,
    private val meals: MealRepository,
    private val weights: WeightRepository,
    private val clock: Clock,
) : ViewModel() {
    private val selection = MutableStateFlow(HistoryPeriod.DAY to clock.today())

    val state: StateFlow<HistoryState> = combine(targets.flow, selection) { data, sel -> data to sel }
        .flatMapLatest { (data, sel) ->
            val (period, anchor) = sel
            val (from, to) = periodRange(period, anchor)
            val base = HistoryState(period = period, anchor = anchor, today = clock.today(), from = from, to = to, data = data)
            if (data == null) {
                flowOf(base)
            } else {
                combine(meals.observeMeals(data.profile.id, from, to), weights.observeRecords(data.profile.id)) { list, records ->
                    val summaries = NutritionStats.dailySummaries(list, from, to)
                    base.copy(
                        meals = list,
                        summaries = summaries,
                        average = NutritionStats.averageOfLoggedDays(summaries),
                        weights = records.filter { !it.date.isBefore(from) && !it.date.isAfter(to) },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState())

    fun setPeriod(period: HistoryPeriod) = selection.update { period to it.second }

    fun move(steps: Long) = selection.update { (period, anchor) ->
        val next = when (period) {
            HistoryPeriod.DAY -> anchor.plusDays(steps)
            HistoryPeriod.WEEK -> anchor.plusWeeks(steps)
            HistoryPeriod.MONTH -> anchor.plusMonths(steps)
        }
        val today = clock.today()
        period to if (next.isAfter(today)) today else next
    }
}
