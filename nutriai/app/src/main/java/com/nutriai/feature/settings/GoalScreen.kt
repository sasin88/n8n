package com.nutriai.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.nutriai.core.ui.components.SectionTitle
import com.nutriai.core.ui.components.ValueRow
import com.nutriai.core.ui.format.Labels
import com.nutriai.data.repository.Clock
import com.nutriai.domain.health.TargetValidation
import com.nutriai.domain.health.WeightGoalPlanner
import com.nutriai.domain.health.WeightPace
import com.nutriai.domain.model.ActivityLevel
import com.nutriai.domain.model.Goal
import com.nutriai.domain.model.UserProfile
import com.nutriai.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class GoalViewModel @Inject constructor(private val profiles: ProfileRepository, val clock: Clock) : ViewModel() {
    val profile: StateFlow<UserProfile?> = profiles.observeActiveProfile().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    fun save(p: UserProfile, then: () -> Unit) = viewModelScope.launch { runCatching { profiles.update(p) }.onSuccess { then() } }
}

@Composable
fun GoalScreen(onBack: () -> Unit, viewModel: GoalViewModel = hiltViewModel()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val p = profile ?: return
    var goal by remember { mutableStateOf(p.goal) }
    var target by remember { mutableStateOf(p.targetWeightKg ?: p.weightKg) }
    var pace by remember { mutableStateOf(WeightPace.entries.firstOrNull { it.kgPerWeek == p.weeklyRateKg } ?: WeightPace.RECOMMENDED) }
    var activity by remember { mutableStateOf(p.activityLevel) }
    LaunchedEffect(p.id) { goal = p.goal; target = p.targetWeightKg ?: p.weightKg; activity = p.activityLevel }
    val needsTarget = goal != Goal.MAINTAIN_WEIGHT
    val validation = WeightGoalPlanner.validateTarget(p.weightKg, target, p.heightCm, goal)
    val plan = if (needsTarget && validation == TargetValidation.Ok) WeightGoalPlanner.plan(p.weightKg, target, goal, pace.kgPerWeek, viewModel.clock.today()) else null

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            GlassIconButton(onClick = onBack, contentDescription = "Volver") { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
            LargeTitle("Mi meta")
        }
        item {
            SectionTitle("Meta principal")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Goal.entries) { g -> GlassChip(Labels.goal(g), g == goal, { goal = g }) }
            }
        }
        if (needsTarget) {
            item {
                NutriCard {
                    ValueRow("Peso inicial", Labels.weight(p.weightKg))
                    ValueRow("Peso objetivo", Labels.weight(target))
                    Slider(
                        value = target.toFloat(), onValueChange = { target = (it * 2).roundToInt() / 2.0 },
                        valueRange = 30f..250f,
                    )
                }
            }
            item {
                SectionTitle("Ritmo")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(WeightPace.entries) { pc -> GlassChip("${Labels.formatNumber(pc.kgPerWeek)} kg/sem", pc == pace, { pace = pc }) }
                }
            }
            item {
                when (validation) {
                    is TargetValidation.BelowHealthy -> NoticeCard("Ese objetivo está por debajo del rango saludable. Mínimo: ${Labels.weight(validation.minimumKg)}.", isWarning = true)
                    TargetValidation.WrongDirection -> NoticeCard("El objetivo no coincide con la meta elegida.", isWarning = true)
                    TargetValidation.Ok -> plan?.let {
                        NutriCard {
                            ValueRow("Fecha estimada", "${Labels.shortDate(it.estimatedDate)} ${it.estimatedDate.year}")
                            ValueRow("Ajuste diario", "${if (it.dailyAdjustmentKcal > 0) "+" else ""}${it.dailyAdjustmentKcal} kcal")
                        }
                    }
                }
            }
        }
        item {
            SectionTitle("Nivel de actividad")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ActivityLevel.entries) { a -> GlassChip(Labels.activityShort(a), a == activity, { activity = a }) }
            }
            Text(Labels.activity(activity), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            PrimaryButton(
                "Guardar",
                {
                    viewModel.save(
                        p.copy(
                            goal = goal, activityLevel = activity,
                            targetWeightKg = if (needsTarget) target else null,
                            weeklyRateKg = if (needsTarget) pace.kgPerWeek else null,
                        ),
                        onBack,
                    )
                },
                enabled = !needsTarget || validation == TargetValidation.Ok,
            )
        }
    }
}
