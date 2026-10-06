package com.nutriai.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.nutriai.core.ui.theme.NutriAiTheme
import com.nutriai.domain.model.ActivityLevel
import com.nutriai.domain.model.FoodReference
import com.nutriai.domain.model.Goal
import com.nutriai.domain.model.MealOrigin
import com.nutriai.domain.model.MealType
import com.nutriai.domain.model.NutrientBasis
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.model.NutritionSource
import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.model.Sex
import com.nutriai.domain.model.UserProfile
import com.nutriai.domain.nutrition.CalorieTargetCalculator
import com.nutriai.feature.common.ProfileWithTarget
import com.nutriai.feature.dashboard.DashboardContent
import com.nutriai.feature.dashboard.DashboardState
import com.nutriai.feature.meal.DraftItem
import com.nutriai.feature.meal.EditorMode
import com.nutriai.feature.meal.EditorPhase
import com.nutriai.feature.meal.MealDraft
import com.nutriai.feature.meal.MealEditorContent
import com.nutriai.feature.meal.MealEditorState
import com.nutriai.feature.profile.ProfileFormContent
import com.nutriai.feature.profile.ProfileFormState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Pruebas de interfaz de los flujos principales (Compose sobre Robolectric). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class UiFlowsTest {
    @get:Rule val compose = createComposeRule()

    private val rice = FoodReference(
        "rice", "Arroz blanco cocido", Nutrients(130.0, 2.7, 28.2, 0.3, 0.4), NutrientBasis.PER_100_G, NutritionSource.USDA_FDC,
    )

    @Test
    fun profileCreation_requiresCompleteDataAndSaves() {
        var saved = false
        compose.setContent {
            var state by remember { mutableStateOf(ProfileFormState()) }
            NutriAiTheme {
                ProfileFormContent(state = state, onChange = { state = it(state) }, onSave = { saved = true }, onBack = null)
            }
        }
        compose.onNodeWithTag("save_profile").performScrollTo().performClick()
        compose.onNodeWithTag("name").performScrollTo()
        compose.onNodeWithText("Escribe un nombre o apodo").assertExists()
        assertTrue(!saved)

        compose.onNodeWithTag("name").performTextInput("Adrian")
        compose.onNodeWithTag("age").performTextInput("35")
        compose.onNodeWithTag("weight").performTextInput("80")
        compose.onNodeWithTag("height").performTextInput("175")
        compose.onNodeWithText("Hombre").performScrollTo().performClick()
        compose.onNodeWithText("Ejercicio moderado (3–5 días/semana)").performScrollTo().performClick()
        compose.onNodeWithText("Perder peso").performScrollTo().performClick()
        compose.onNodeWithTag("save_profile").performScrollTo().performClick()
        assertTrue(saved)
    }

    @Test
    fun dashboard_showsConsumedTargetAndActions() {
        val profile = UserProfile(
            id = 1, name = "Adrian", sex = Sex.MALE, ageYears = 35, weightKg = 80.0, heightCm = 175.0,
            activityLevel = ActivityLevel.MODERATE, goal = Goal.LOSE_WEIGHT, manualCalorieTarget = 2000,
        )
        var analyzed = false
        compose.setContent {
            NutriAiTheme {
                DashboardContent(
                    state = DashboardState(
                        loading = false,
                        data = ProfileWithTarget(profile, CalorieTargetCalculator().calculate(profile)),
                        consumed = Nutrients(1250.0, 60.0, 150.0, 40.0, 10.0),
                    ),
                    onAnalyze = { analyzed = true }, onRegisterMeal = {}, onOpenDay = {}, onOpenProfile = {},
                )
            }
        }
        compose.onNodeWithText("≈ 1.250 kcal", substring = true).assertExists()
        compose.onAllNodesWithText("2.000 kcal", substring = true).onFirst().assertExists()
        compose.onNodeWithTag("analyze").performClick()
        assertTrue(analyzed)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Agua"))
        compose.onNodeWithText("Agua").assertExists()
    }

    @Test
    fun analysisResult_uncertainItemBlocksSaveUntilConfirmed() {
        var saves = 0
        compose.setContent {
            var draft by remember {
                mutableStateOf(
                    MealDraft(
                        type = MealType.LUNCH, date = LocalDate.of(2026, 1, 1), origin = MealOrigin.AI_PHOTO, analysisConfidence = 0.78,
                        items = listOf(DraftItem(1, rice, 150.0, PortionUnit.GRAM, detectedName = "arroz", aiConfidence = 0.45, needsReview = true)),
                    ),
                )
            }
            NutriAiTheme {
                MealEditorContent(
                    state = MealEditorState(mode = EditorMode.ANALYSIS, phase = EditorPhase.Editing, draft = draft, isMockAnalysis = true),
                    onBack = {}, onManual = {}, onTypeChange = {}, onAdd = { _, _, _ -> },
                    onReplace = { _, _, _, _ -> },
                    onConfirm = { key -> draft = draft.copy(items = draft.items.map { if (it.key == key) it.copy(needsReview = false) else it }) },
                    onRemove = {}, onExpand = {}, onSave = { saves++ }, onDelete = {},
                    search = { emptyList() }, createFood = { _, _, _ -> },
                )
            }
        }
        compose.onNodeWithText("Confianza del análisis").assertExists()
        compose.onNodeWithText("78%").assertExists()
        compose.onNodeWithText("Modo demostración").assertExists()
        compose.onNodeWithText("No estoy completamente seguro", substring = true).assertExists()
        compose.onNodeWithTag("save_meal").assertIsNotEnabled()

        compose.onNodeWithTag("confirm_item").performClick()
        compose.onNodeWithTag("save_meal").assertIsEnabled().performClick()
        assertEquals(1, saves)
        compose.onNodeWithTag("total_kcal").assertExists()
    }

    @Test
    fun onboarding_walksThroughStepsAndShowsPlan() {
        var saved = false
        compose.setContent {
            var state by remember { mutableStateOf(com.nutriai.feature.onboarding.OnboardingState()) }
            fun next() {
                val steps = state.steps
                state = state.copy(step = steps[(steps.indexOf(state.step) + 1).coerceAtMost(steps.size - 1)])
            }
            NutriAiTheme {
                com.nutriai.feature.onboarding.OnboardingFlowContent(
                    state = state,
                    onChange = { state = it(state) },
                    onNext = { next() },
                    onBack = {},
                    canContinue = true,
                    onCalculated = { state = state.copy(step = com.nutriai.feature.onboarding.OnboardingStep.PLAN) },
                    onSave = { saved = true },
                    today = LocalDate.of(2026, 1, 1),
                )
            }
        }
        compose.onNodeWithTag("onboarding_next").performClick() // bienvenida
        compose.onNodeWithTag("onboarding_name").performTextInput("Adrian")
        compose.onNodeWithTag("onboarding_next").performClick()
        compose.onNodeWithText("Perder peso").performClick()
        compose.onNodeWithTag("onboarding_next").performClick()
        compose.onNodeWithText("Omitir").assertExists()
    }

    @Test
    fun manualRegistration_addFoodOpensSearch() {
        compose.setContent {
            NutriAiTheme {
                MealEditorContent(
                    state = MealEditorState(
                        mode = EditorMode.NEW, phase = EditorPhase.Editing,
                        draft = MealDraft(type = MealType.BREAKFAST, date = LocalDate.of(2026, 1, 1), origin = MealOrigin.MANUAL),
                    ),
                    onBack = {}, onManual = {}, onTypeChange = {}, onAdd = { _, _, _ -> }, onReplace = { _, _, _, _ -> },
                    onConfirm = {}, onRemove = {}, onExpand = {}, onSave = {}, onDelete = {},
                    search = { listOf(rice) }, createFood = { _, _, _ -> },
                )
            }
        }
        compose.onNodeWithTag("save_meal").assertIsNotEnabled()
        compose.onNodeWithTag("add_food").performClick()
        compose.onNodeWithText("¿Qué comiste?").assertExists()
    }
}
