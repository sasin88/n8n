package com.nutriai.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriai.data.repository.Clock
import com.nutriai.data.settings.SettingsRepository
import com.nutriai.domain.health.BodyAnalysis
import com.nutriai.domain.health.Hydration
import com.nutriai.domain.health.TargetValidation
import com.nutriai.domain.health.WeightGoalPlanner
import com.nutriai.domain.health.WeightPace
import com.nutriai.domain.health.WeightPlan
import com.nutriai.domain.model.ActivityLevel
import com.nutriai.domain.model.Goal
import com.nutriai.domain.model.SecondaryGoal
import com.nutriai.domain.model.Sex
import com.nutriai.domain.model.UnitSystem
import com.nutriai.domain.model.UserProfile
import com.nutriai.domain.nutrition.CalorieTarget
import com.nutriai.domain.nutrition.CalorieTargetCalculator
import com.nutriai.domain.nutrition.EnergyFormulas
import com.nutriai.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep {
    WELCOME, NAME, GOAL, SECONDARY, SEX, AGE, HEIGHT, WEIGHT, ANALYSIS, TARGET, ACTIVITY, PACE, CALCULATING, PLAN
}

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val name: String = "",
    val goal: Goal? = null,
    val improveHealth: Boolean = false,
    val secondary: Set<SecondaryGoal> = emptySet(),
    val sex: Sex? = null,
    val age: Int = 30,
    val heightCm: Double = 165.0,
    val weightKg: Double = 70.0,
    val targetKg: Double = 65.0,
    val activity: ActivityLevel? = null,
    val pace: WeightPace = WeightPace.RECOMMENDED,
    val units: UnitSystem = UnitSystem.METRIC,
    val saving: Boolean = false,
    val done: Boolean = false,
    val error: String? = null,
) {
    val needsTarget: Boolean get() = goal != null && goal != Goal.MAINTAIN_WEIGHT
    val bmi get() = BodyAnalysis.analyze(weightKg, heightCm)
    val targetValidation: TargetValidation
        get() = goal?.let { WeightGoalPlanner.validateTarget(weightKg, targetKg, heightCm, it) } ?: TargetValidation.Ok

    /** Pasos visibles según las respuestas (sin objetivo de peso no se pregunta el objetivo ni el ritmo). */
    val steps: List<OnboardingStep>
        get() = OnboardingStep.entries.filter { s ->
            when (s) {
                OnboardingStep.TARGET, OnboardingStep.PACE -> needsTarget
                else -> true
            }
        }

    val progress: Float get() = (steps.indexOf(step).coerceAtLeast(0)).toFloat() / (steps.size - 1)

    fun profile(): UserProfile = UserProfile(
        name = name.trim(),
        sex = sex ?: Sex.FEMALE,
        ageYears = age,
        weightKg = weightKg,
        heightCm = heightCm,
        activityLevel = activity ?: ActivityLevel.LIGHT,
        goal = goal ?: Goal.MAINTAIN_WEIGHT,
        targetWeightKg = if (needsTarget) targetKg else null,
        unitSystem = units,
        secondaryGoals = secondary,
        weeklyRateKg = if (needsTarget) pace.kgPerWeek else null,
    )

    fun target(): CalorieTarget = CalorieTargetCalculator(EnergyFormulas.default).calculate(profile())

    fun plan(today: java.time.LocalDate): WeightPlan? =
        if (needsTarget) WeightGoalPlanner.plan(weightKg, targetKg, goal!!, pace.kgPerWeek, today) else null

    val waterGoalMl: Int get() = Hydration.suggestedGoalMl(weightKg)
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val settings: SettingsRepository,
    val clock: Clock,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun update(transform: (OnboardingState) -> OnboardingState) = _state.update(transform)

    fun canContinue(s: OnboardingState): Boolean = when (s.step) {
        OnboardingStep.NAME -> s.name.isNotBlank() && s.name.trim().length <= 30
        OnboardingStep.GOAL -> s.goal != null
        OnboardingStep.SEX -> s.sex != null
        OnboardingStep.AGE -> s.age in 13..100
        OnboardingStep.HEIGHT -> s.heightCm in 100.0..250.0
        OnboardingStep.WEIGHT -> s.weightKg in 25.0..300.0
        OnboardingStep.TARGET -> s.targetValidation == TargetValidation.Ok
        OnboardingStep.ACTIVITY -> s.activity != null
        OnboardingStep.CALCULATING -> false
        else -> true
    }

    fun next() = _state.update { s ->
        val steps = s.steps
        val i = steps.indexOf(s.step)
        val nextStep = steps.getOrNull(i + 1) ?: s.step
        // Al llegar al peso objetivo, se propone un valor razonable dentro del rango saludable.
        val proposedTarget = if (nextStep == OnboardingStep.TARGET && s.step != OnboardingStep.TARGET) suggestTarget(s) else s.targetKg
        s.copy(step = nextStep, targetKg = proposedTarget)
    }

    fun back() = _state.update { s ->
        val steps = s.steps
        val i = steps.indexOf(s.step)
        s.copy(step = steps.getOrNull(i - 1) ?: s.step)
    }

    fun finishCalculating() = _state.update { it.copy(step = OnboardingStep.PLAN) }

    fun save() {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching { profiles.create(s.profile()) }
                .onSuccess { _state.update { it.copy(saving = false, done = true) } }
                .onFailure { _state.update { it.copy(saving = false, error = "No pudimos guardar tu perfil. Inténtalo de nuevo.") } }
        }
    }

    private fun suggestTarget(s: OnboardingState): Double {
        val healthy = BodyAnalysis.healthyWeightRange(s.heightCm)
        val raw = when (s.goal) {
            Goal.LOSE_WEIGHT -> maxOf(healthy.endInclusive, s.weightKg * 0.9).coerceAtMost(s.weightKg - 1)
            Goal.GAIN_WEIGHT, Goal.GAIN_MUSCLE -> s.weightKg + 3
            else -> s.weightKg
        }
        return (raw * 2).roundToInt() / 2.0
    }
}
