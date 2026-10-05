package com.nutriai.domain.nutrition

import com.nutriai.domain.TestData
import com.nutriai.domain.model.ActivityLevel
import com.nutriai.domain.model.Goal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieTargetCalculatorTest {
    private val calculator = CalorieTargetCalculator()

    @Test
    fun `tdee applies activity multiplier`() {
        val target = calculator.calculate(TestData.adrian)
        assertEquals(1723.75 * 1.55, target.tdeeKcal, 0.001)
    }

    @Test
    fun `weight loss subtracts 500 kcal`() {
        val target = calculator.calculate(TestData.adrian)
        assertEquals(2172, target.suggestedKcal)
        assertEquals(2172, target.effectiveKcal)
        assertFalse(target.isManual)
        assertFalse(target.minimumApplied)
    }

    @Test
    fun `maintenance keeps tdee`() {
        val target = calculator.calculate(TestData.maria)
        assertEquals(Math.round(1370.25 * 1.375).toInt(), target.suggestedKcal)
    }

    @Test
    fun `suggestion never goes below safety minimum`() {
        val tiny = TestData.maria.copy(weightKg = 40.0, heightCm = 145.0, ageYears = 70, activityLevel = ActivityLevel.SEDENTARY, goal = Goal.LOSE_WEIGHT)
        val target = calculator.calculate(tiny)
        assertEquals(1200, target.suggestedKcal)
        assertTrue(target.minimumApplied)
    }

    @Test
    fun `manual target overrides suggestion and is flagged when below minimum`() {
        val target = calculator.calculate(TestData.adrian.copy(manualCalorieTarget = 1400))
        assertEquals(1400, target.effectiveKcal)
        assertTrue(target.isManual)
        assertTrue(target.manualBelowMinimum)
    }

    @Test
    fun `macro targets add up to the calorie target`() {
        val macros = CalorieTargetCalculator.macroTargets(2000, Goal.MAINTAIN_WEIGHT)
        val kcal = macros.proteinG * 4 + macros.carbohydratesG * 4 + macros.fatG * 9
        assertEquals(2000.0, kcal, 0.001)
        assertEquals(125.0, macros.proteinG, 0.001)
    }

    @Test
    fun `alternative formula can be injected`() {
        val target = CalorieTargetCalculator(RevisedHarrisBenedictFormula).calculate(TestData.adrian)
        assertEquals(1801.252, target.bmrKcal, 0.001)
    }
}
