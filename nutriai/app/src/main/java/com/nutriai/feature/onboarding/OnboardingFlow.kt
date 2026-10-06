package com.nutriai.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutriai.core.ui.components.AppearIn
import com.nutriai.core.ui.components.GlassIconButton
import com.nutriai.core.ui.components.GlassSegmented
import com.nutriai.core.ui.components.LineChart
import com.nutriai.core.ui.components.NoticeCard
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.core.ui.components.PrimaryButton
import com.nutriai.core.ui.components.ValueRow
import com.nutriai.core.ui.components.glass
import com.nutriai.core.ui.format.Labels
import com.nutriai.core.ui.theme.CarbsColor
import com.nutriai.core.ui.theme.FatColor
import com.nutriai.core.ui.theme.ProteinColor
import com.nutriai.domain.health.BmiCategory
import com.nutriai.domain.health.BodyAnalysis
import com.nutriai.domain.health.GoalDifficulty
import com.nutriai.domain.health.TargetValidation
import com.nutriai.domain.health.WeightGoalPlanner
import com.nutriai.domain.health.WeightPace
import com.nutriai.domain.model.ActivityLevel
import com.nutriai.domain.model.Goal
import com.nutriai.domain.model.SecondaryGoal
import com.nutriai.domain.model.Sex
import com.nutriai.domain.model.UnitSystem
import com.nutriai.domain.model.Units
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun OnboardingFlowScreen(onDone: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    OnboardingFlowContent(
        state = state,
        onChange = viewModel::update,
        onNext = viewModel::next,
        onBack = viewModel::back,
        canContinue = viewModel.canContinue(state),
        onCalculated = viewModel::finishCalculating,
        onSave = viewModel::save,
        today = viewModel.clock.today(),
    )
}

@Composable
fun OnboardingFlowContent(
    state: OnboardingState,
    onChange: ((OnboardingState) -> OnboardingState) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    canContinue: Boolean,
    onCalculated: () -> Unit,
    onSave: () -> Unit,
    today: java.time.LocalDate,
) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 20.dp)) {
        if (state.step != OnboardingStep.WELCOME && state.step != OnboardingStep.CALCULATING) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.step != OnboardingStep.PLAN) {
                    GlassIconButton(onClick = onBack, contentDescription = "Atrás") {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                    Spacer(Modifier.width(16.dp))
                }
                val progress by animateFloatAsState(state.progress, tween(400), label = "progress")
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {},
                    gapSize = 0.dp,
                )
            }
        }
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(tween(320)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(320))) togetherWith
                    (slideOutHorizontally(tween(260)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(200)))
            },
            modifier = Modifier.weight(1f),
            label = "step",
        ) { step ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                when (step) {
                    OnboardingStep.WELCOME -> Welcome()
                    OnboardingStep.NAME -> NameStep(state, onChange)
                    OnboardingStep.GOAL -> GoalStep(state, onChange)
                    OnboardingStep.SECONDARY -> SecondaryStep(state, onChange)
                    OnboardingStep.SEX -> SexStep(state, onChange)
                    OnboardingStep.AGE -> AgeStep(state, onChange)
                    OnboardingStep.HEIGHT -> HeightStep(state, onChange)
                    OnboardingStep.WEIGHT -> WeightStep(state, onChange)
                    OnboardingStep.ANALYSIS -> AnalysisStep(state)
                    OnboardingStep.TARGET -> TargetStep(state, onChange)
                    OnboardingStep.ACTIVITY -> ActivityStep(state, onChange)
                    OnboardingStep.PACE -> PaceStep(state, onChange, today)
                    OnboardingStep.CALCULATING -> Calculating(state, onCalculated)
                    OnboardingStep.PLAN -> PlanStep(state, today)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        when (state.step) {
            OnboardingStep.CALCULATING -> Unit
            OnboardingStep.PLAN -> {
                state.error?.let { NoticeCard(it, isWarning = true, modifier = Modifier.padding(bottom = 8.dp)) }
                PrimaryButton("Empezar", onSave, enabled = !state.saving, modifier = Modifier.padding(bottom = 12.dp).testTag("onboarding_start"))
            }
            OnboardingStep.WELCOME -> PrimaryButton("Crear mi plan", onNext, modifier = Modifier.padding(bottom = 12.dp).testTag("onboarding_next"))
            OnboardingStep.SECONDARY -> PrimaryButton(if (state.secondary.isEmpty()) "Omitir" else "Continuar", onNext, modifier = Modifier.padding(bottom = 12.dp).testTag("onboarding_next"))
            else -> PrimaryButton("Continuar", onNext, enabled = canContinue, modifier = Modifier.padding(bottom = 12.dp).testTag("onboarding_next"))
        }
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String? = null) {
    Spacer(Modifier.height(16.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium)
    if (subtitle != null) {
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun OptionRow(text: String, selected: Boolean, emoji: String? = null, detail: String? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val selectedBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).glass(shape, onClick = onClick)
            .then(if (selected) Modifier.background(selectedBg) else Modifier)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (emoji != null) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            if (detail != null) Text(detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Welcome() {
    Spacer(Modifier.height(48.dp))
    AppearIn(0) { Text("🥗", style = MaterialTheme.typography.displayLarge) }
    Spacer(Modifier.height(16.dp))
    AppearIn(1) { Text("Come mejor, sin complicarte", style = MaterialTheme.typography.displaySmall) }
    Spacer(Modifier.height(12.dp))
    AppearIn(2) {
        Text(
            "Fotografía tu plato, revisa la estimación y lleva tu progreso. Sin cuentas ni contraseñas: todo se queda en este teléfono.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(28.dp))
    AppearIn(3) {
        NutriCard {
            Feature("📷", "Analiza tu comida", "La IA identifica los alimentos y estima porciones.")
            Feature("💧", "Agua, ejercicio y ayuno", "Todo tu día en un solo lugar.")
            Feature("📈", "Progreso real", "IMC, peso y tendencias semana a semana.")
        }
    }
    Spacer(Modifier.height(16.dp))
    NoticeCard("Las calorías son estimaciones orientativas. NutriAI no sustituye a un médico, nutricionista o dietista.")
}

@Composable
private fun Feature(emoji: String, title: String, body: String) {
    Row(Modifier.padding(vertical = 8.dp)) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NameStep(state: OnboardingState, onChange: ((OnboardingState) -> OnboardingState) -> Unit) {
    StepTitle("¿Cómo te llamamos?", "Un nombre o apodo. Solo se usa dentro de la app.")
    OutlinedTextField(
        value = state.name,
        onValueChange = { v -> onChange { it.copy(name = v.take(30)) } },
        placeholder = { Text("Tu nombre") },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        modifier = Modifier.fillMaxWidth().testTag("onboarding_name"),
    )
}

@Composable
private fun GoalStep(state: OnboardingState, onChange: ((OnboardingState) -> OnboardingState) -> Unit) {
    StepTitle("¿Cuál es tu objetivo principal?")
    OptionRow("Perder peso", state.goal == Goal.LOSE_WEIGHT && !state.improveHealth, "⚖️") { onChange { it.copy(goal = Goal.LOSE_WEIGHT, improveHealth = false) } }
    OptionRow("Mantener mi peso", state.goal == Goal.MAINTAIN_WEIGHT && !state.improveHealth, "🎯") { onChange { it.copy(goal = Goal.MAINTAIN_WEIGHT, improveHealth = false) } }
    OptionRow("Ganar peso", state.goal == Goal.GAIN_WEIGHT, "📈") { onChange { it.copy(goal = Goal.GAIN_WEIGHT, improveHealth = false) } }
    OptionRow("Ganar masa muscular", state.goal == Goal.GAIN_MUSCLE, "💪") { onChange { it.copy(goal = Goal.GAIN_MUSCLE, improveHealth = false) } }
    OptionRow("Mejorar mi salud en general", state.improveHealth, "🌿", "Sin cambiar de peso") {
        onChange { it.copy(goal = Goal.MAINTAIN_WEIGHT, improveHealth = true) }
    }
}

private val secondaryOptions = listOf(
    Triple(SecondaryGoal.GET_FIT, "Ponerme en forma", "🏃"),
    Triple(SecondaryGoal.MORE_ENERGY, "Tener más energía", "⚡"),
    Triple(SecondaryGoal.BETTER_SLEEP, "Dormir mejor", "😴"),
    Triple(SecondaryGoal.AVOID_ADDITIVES, "Evitar aditivos o alérgenos", "🚫"),
    Triple(SecondaryGoal.SKIN_AND_AGING, "Cuidar mi piel", "✨"),
    Triple(SecondaryGoal.IMMUNE_SYSTEM, "Reforzar mis defensas", "🛡️"),
    Triple(SecondaryGoal.MANAGE_CONDITIONS, "Acompañar una condición de salud", "🩺"),
)

@Composable
private fun SecondaryStep(state: OnboardingState, onChange: ((OnboardingState) -> OnboardingState) -> Unit) {
    StepTitle("¿Algo más que quieras lograr?", "Puedes elegir varias o ninguna.")
    secondaryOptions.forEach { (goal, label, emoji) ->
        OptionRow(label, goal in state.secondary, emoji) {
            onChange { s -> s.copy(secondary = if (goal in s.secondary) s.secondary - goal else s.secondary + goal) }
        }
    }
    if (SecondaryGoal.MANAGE_CONDITIONS in state.secondary) {
        Spacer(Modifier.height(8.dp))
        NoticeCard(
            "Si tienes una condición médica (por ejemplo diabetes, enfermedad renal o un trastorno de la conducta alimentaria), sigue las indicaciones de tu profesional de salud. NutriAI no da recomendaciones médicas.",
            isWarning = true,
        )
    }
}

@Composable
private fun SexStep(state: OnboardingState, onChange: ((OnboardingState) -> OnboardingState) -> Unit) {
    StepTitle("¿Cuál es tu sexo?", "Se usa solo para estimar tu gasto energético con fórmulas reconocidas.")
    OptionRow("Hombre", state.sex == Sex.MALE, "👨") { onChange { it.copy(sex = Sex.MALE) } }
    OptionRow("Mujer", state.sex == Sex.FEMALE, "👩") { onChange { it.copy(sex = Sex.FEMALE) } }
}

/** Valor grande con botones − / + y deslizador, al estilo de un selector táctil. */
@Composable
private fun BigValuePicker(
    display: String,
    unit: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    onValue: (Float) -> Unit,
    tag: String,
) {
    NutriCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            GlassIconButton(onClick = { onValue((value - step).coerceIn(range)) }, contentDescription = "Menos") { Icon(Icons.Rounded.Remove, null) }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(display, style = MaterialTheme.typography.displayLarge, modifier = Modifier.testTag(tag))
                Spacer(Modifier.width(6.dp))
                Text(unit, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 10.dp))
            }
            GlassIconButton(onClick = { onValue((value + step).coerceIn(range)) }, contentDescription = "Más") { Icon(Icons.Rounded.Add, null) }
        }
        Spacer(Modifier.height(12.dp))
        Slider(value = value.coerceIn(range), onValueChange = { onValue((it / step).roundToInt() * step) }, valueRange = range)
    }
}

@Composable
private fun AgeStep(state: OnboardingState, onChange: ((OnboardingState) -> OnboardingState) -> Unit) {
    StepTitle("¿Qué edad tienes?", "La edad cambia el metabolismo basal.")
    BigValuePicker(state.age.toString(), "años", state.age.toFloat(), 13f..100f, 1f, { v -> onChange { it.copy(age = v.roundToInt()) } }, "age_value")
    if (state.age < 18) {
        Spacer(Modifier.height(12.dp))
        NoticeCard("Si eres menor de 18 años, tus necesidades son distintas. Consulta con un profesional de la salud antes de seguir un objetivo calórico.", isWarning = true)
    }
}

@Composable
private fun UnitToggle(state: OnboardingState, onChange: ((OnboardingState) -> OnboardingState) -> Unit, metricLabel: String, imperialLabel: String) {
    GlassSegmented(
        options = listOf(metricLabel, imperialLabel),
        selectedIndex = if (state.units == UnitSystem.METRIC) 0 else 1,
        onSelect = { i -> onChange { it.copy(units = if (i == 0) UnitSystem.METRIC else UnitSystem.IMPERIAL) } },
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun HeightStep(state: OnboardingState, onChange: ((OnboardingState) -> OnboardingState) -> Unit) {
    StepTitle("¿Cuánto mides?", "La altura y el peso se usan para estimar tu tasa metabólica basal.")
    UnitToggle(state, onChange, "cm", "pies")
    if (state.units == UnitSystem.METRIC) {
        BigValuePicker(state.heightCm.roundToInt().toString(), "cm", state.heightCm.toFloat(), 100f..250f, 1f, { v -> onChange { it.copy(heightCm = v.toDouble()) } }, "height_value")
    } else {
        val (ft, inch) = Units.cmToFeetInches(state.heightCm)
        val totalIn = (ft * 12 + inch).toFloat()
        BigValuePicker("$ft′$inch″", "", totalIn, 40f..98f, 1f, { v -> onChange { it.copy(heightCm = v * Units.CM_PER_INCH) } }, "height_value")
    }
}

@Composable
private fun WeightStep(state: OnboardingState, onChange: ((OnboardingState) -> OnboardingState) -> Unit) {
    StepTitle("¿Cuánto pesas hoy?", "Puedes actualizarlo cuando quieras desde Progreso.")
    UnitToggle(state, onChange, "kg", "lb")
    if (state.units == UnitSystem.METRIC) {
        BigValuePicker(Labels.formatNumber(state.weightKg), "kg", state.weightKg.toFloat(), 25f..300f, 0.5f, { v -> onChange { it.copy(weightKg = v.toDouble()) } }, "weight_value")
    } else {
        val lb = Units.kgToLb(state.weightKg)
        BigValuePicker(lb.roundToInt().toString(), "lb", lb.toFloat(), 55f..660f, 1f, { v -> onChange { it.copy(weightKg = Units.lbToKg(v.toDouble())) } }, "weight_value")
    }
}

/** Escala de IMC con zonas de color y un marcador. */
@Composable
fun BmiGauge(bmi: Double, modifier: Modifier = Modifier) {
    val min = 12.0
    val max = 42.0
    val fraction = ((bmi - min) / (max - min)).toFloat().coerceIn(0f, 1f)
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            val barH = 10.dp.toPx()
            val y = size.height - barH
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(Color(0xFF32ADE6), Color(0xFF30D158), Color(0xFFFFD60A), Color(0xFFFF9F0A), Color(0xFFFF453A))),
                topLeft = Offset(0f, y), size = androidx.compose.ui.geometry.Size(size.width, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barH / 2),
            )
            val x = size.width * fraction
            drawCircle(Color.White, radius = 9.dp.toPx(), center = Offset(x, y + barH / 2))
            drawCircle(onSurface, radius = 5.dp.toPx(), center = Offset(x, y + barH / 2))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Bajo", "Saludable", "Sobrepeso", "Obesidad").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun bmiLabel(category: BmiCategory) = when (category) {
    BmiCategory.UNDERWEIGHT -> "Peso bajo"
    BmiCategory.HEALTHY -> "Saludable"
    BmiCategory.OVERWEIGHT -> "Sobrepeso"
    BmiCategory.OBESITY -> "Obesidad"
}

@Composable
private fun AnalysisStep(state: OnboardingState) {
    val bmi = state.bmi
    StepTitle("Tu análisis personal", "Basado en el índice de masa corporal (IMC).")
    NutriCard {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(String.format("%.1f", bmi.bmi), style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.width(10.dp))
            Text("IMC · ${bmiLabel(bmi.category)}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
        }
        Spacer(Modifier.height(12.dp))
        BmiGauge(bmi.bmi)
        Spacer(Modifier.height(16.dp))
        ValueRow("IMC saludable", "18.5 – 24.9")
        ValueRow("Peso saludable para tu altura", "${weight(state, bmi.healthyWeightKg.start)} – ${weight(state, bmi.healthyWeightKg.endInclusive)}")
        when {
            bmi.weightOutsideAbove(state.weightKg) -> ValueRow("Para llegar al rango saludable", "−${weight(state, state.weightKg - bmi.healthyWeightKg.endInclusive)}")
            state.weightKg < bmi.healthyWeightKg.start -> ValueRow("Para llegar al rango saludable", "+${weight(state, bmi.healthyWeightKg.start - state.weightKg)}")
            else -> Unit
        }
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "El IMC es un indicador general: no distingue músculo de grasa y no es un diagnóstico.",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (bmi.category == BmiCategory.UNDERWEIGHT) {
        Spacer(Modifier.height(12.dp))
        NoticeCard("Tu IMC está por debajo del rango saludable. Te recomendamos hablar con un profesional de la salud.", isWarning = true)
    }
}

private fun com.nutriai.domain.health.BmiResult.weightOutsideAbove(weightKg: Double) = weightKg > healthyWeightKg.endInclusive

private fun weight(state: OnboardingState, kg: Double): String =
    if (state.units == UnitSystem.METRIC) Labels.weight(kg) else "${Units.kgToLb(kg).roundToInt()} lb"

@Composable
private fun TargetStep(state: OnboardingState, onChange: ((OnboardingState) -> OnboardingState) -> Unit) {
    StepTitle("¿Cuál es tu peso deseado?", "Tu peso objetivo define tu presupuesto diario de calorías.")
    if (state.units == UnitSystem.METRIC) {
        BigValuePicker(Labels.formatNumber(state.targetKg), "kg", state.targetKg.toFloat(), 25f..300f, 0.5f, { v -> onChange { it.copy(targetKg = v.toDouble()) } }, "target_value")
    } else {
        val lb = Units.kgToLb(state.targetKg)
        BigValuePicker(lb.roundToInt().toString(), "lb", lb.toFloat(), 55f..660f, 1f, { v -> onChange { it.copy(targetKg = Units.lbToKg(v.toDouble())) } }, "target_value")
    }
    Spacer(Modifier.height(12.dp))
    when (val v = state.targetValidation) {
        is TargetValidation.BelowHealthy -> NoticeCard(
            "Ese peso queda por debajo del rango saludable para tu altura. El mínimo que la app acepta es ${weight(state, v.minimumKg)}.",
            isWarning = true,
        )
        TargetValidation.WrongDirection -> NoticeCard(
            if (state.goal == Goal.LOSE_WEIGHT) "Para perder peso, el objetivo debe ser menor que tu peso actual." else "Para ganar peso, el objetivo debe ser mayor que tu peso actual.",
            isWarning = true,
        )
        TargetValidation.Ok -> {
            val plan = WeightGoalPlanner.plan(state.weightKg, state.targetKg, state.goal!!, state.pace.kgPerWeek, java.time.LocalDate.now())
            val (title, body) = when (plan.difficulty) {
                GoalDifficulty.EASY -> "Meta alcanzable" to "Cambiarás un ${plan.changePercent.roundToInt()}% de tu peso."
                GoalDifficulty.MODERATE -> "Meta realista" to "Cambiarás un ${plan.changePercent.roundToInt()}% de tu peso. Requiere constancia."
                GoalDifficulty.CHALLENGING -> "Meta desafiante" to "Cambiarás un ${plan.changePercent.roundToInt()}% de tu peso. Considera ir por etapas y con acompañamiento profesional."
            }
            NoticeCard(body, title = title, isWarning = plan.difficulty == GoalDifficulty.CHALLENGING)
        }
    }
}

@Composable
private fun ActivityStep(state: OnboardingState, onChange: ((OnboardingState) -> OnboardingState) -> Unit) {
    StepTitle("¿Cuánto te mueves en un día normal?")
    val emoji = mapOf(
        ActivityLevel.SEDENTARY to "🪑", ActivityLevel.LIGHT to "🚶", ActivityLevel.MODERATE to "🚴",
        ActivityLevel.ACTIVE to "🏋️", ActivityLevel.VERY_ACTIVE to "🔥",
    )
    ActivityLevel.entries.forEach { level ->
        OptionRow(Labels.activityShort(level), state.activity == level, emoji[level], Labels.activity(level)) { onChange { it.copy(activity = level) } }
    }
}

@Composable
private fun PaceStep(state: OnboardingState, onChange: ((OnboardingState) -> OnboardingState) -> Unit, today: java.time.LocalDate) {
    StepTitle("¿A qué ritmo?", "Un ritmo moderado es más fácil de mantener.")
    val labels = mapOf(WeightPace.SLOW to "Tranquilo", WeightPace.RECOMMENDED to "Recomendado", WeightPace.FAST to "Rápido")
    WeightPace.entries.forEach { pace ->
        val plan = WeightGoalPlanner.plan(state.weightKg, state.targetKg, state.goal!!, pace.kgPerWeek, today)
        OptionRow(
            "${labels[pace]} · ${Labels.formatNumber(pace.kgPerWeek)} kg/semana",
            state.pace == pace,
            detail = "Meta estimada: ${Labels.shortDate(plan.estimatedDate)} ${plan.estimatedDate.year}",
        ) { onChange { it.copy(pace = pace) } }
    }
    val plan = state.plan(today)!!
    Spacer(Modifier.height(12.dp))
    NutriCard {
        Text("Predecimos que llegarás a ${weight(state, state.targetKg)}", style = MaterialTheme.typography.titleMedium)
        Text("hacia el ${Labels.shortDate(plan.estimatedDate)} de ${plan.estimatedDate.year}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LineChart(WeightGoalPlanner.projection(state.weightKg, state.targetKg, plan.weeks), height = 140.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Hoy · ${weight(state, state.weightKg)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Meta · ${weight(state, state.targetKg)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "El cambio suele ser más lento al principio. Es una estimación: el resultado real depende de muchos factores.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Calculating(state: OnboardingState, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(2800, easing = LinearEasing))
        delay(250)
        onDone()
    }
    val items = listOf("Calculando tu metabolismo", "Evaluando tu punto de partida", "Ajustando tu presupuesto diario", "Preparando tu plan")
    Column(Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator(
                progress = { progress.value }, modifier = Modifier.size(160.dp), strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), strokeCap = StrokeCap.Round, gapSize = 0.dp,
            )
            Text("${(progress.value * 100).roundToInt()}%", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(28.dp))
        Text("Personalizando tu plan", style = MaterialTheme.typography.headlineSmall)
        Text("${Labels.weight(state.weightKg)} · ${state.heightCm.roundToInt()} cm · ${state.age} años", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        items.forEachIndexed { i, label ->
            val done = progress.value > (i + 1) / items.size.toFloat() - 0.01f
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                Spacer(Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge, color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PlanStep(state: OnboardingState, today: java.time.LocalDate) {
    val target = state.target()
    val plan = state.plan(today)
    StepTitle("Tu plan está listo, ${state.name.trim()}", "Puedes ajustarlo en cualquier momento desde tu perfil.")
    NutriCard {
        Text("PRESUPUESTO DIARIO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Labels.formatNumber(target.effectiveKcal.toDouble()), style = MaterialTheme.typography.displayLarge, modifier = Modifier.testTag("plan_kcal"))
            Spacer(Modifier.width(6.dp))
            Text("kcal", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 10.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MacroPill("Proteínas", target.macros.proteinG, ProteinColor)
            MacroPill("Carbos", target.macros.carbohydratesG, CarbsColor)
            MacroPill("Grasas", target.macros.fatG, FatColor)
        }
    }
    Spacer(Modifier.height(12.dp))
    NutriCard {
        ValueRow("Agua recomendada", "${Labels.formatNumber(state.waterGoalMl / 1000.0)} L al día")
        if (plan != null) {
            ValueRow("Ritmo", "${Labels.formatNumber(plan.kgPerWeek)} kg/semana")
            ValueRow("Meta estimada", "${Labels.shortDate(plan.estimatedDate)} ${plan.estimatedDate.year}")
        }
        ValueRow("Metabolismo basal", Labels.approxKcal(target.bmrKcal))
        ValueRow("Gasto diario estimado", Labels.approxKcal(target.tdeeKcal))
    }
    if (target.minimumApplied) {
        Spacer(Modifier.height(12.dp))
        NoticeCard("Usamos un mínimo orientativo de seguridad para tu presupuesto. Un profesional puede ayudarte a afinarlo.", isWarning = true)
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "Estimación basada en la fórmula Mifflin-St Jeor y en factores de actividad habituales. No es un consejo médico.",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
    )
}

@Composable
private fun MacroPill(label: String, grams: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${grams.roundToInt()} g", style = MaterialTheme.typography.titleLarge, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

