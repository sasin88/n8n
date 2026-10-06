package com.nutriai.domain.health

import com.nutriai.domain.model.Goal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyAnalysisTest {
    @Test fun `bmi matches reference example`() {
        // 89.3 kg, 150 cm -> 39.7 (mismo ejemplo que en la referencia de WiseMeal)
        assertEquals(39.7, BodyAnalysis.bmi(89.3, 150.0), 0.05)
        assertEquals(BmiCategory.OBESITY, BodyAnalysis.analyze(89.3, 150.0).category)
    }

    @Test fun `categories follow WHO cutoffs`() {
        assertEquals(BmiCategory.UNDERWEIGHT, BodyAnalysis.category(18.4))
        assertEquals(BmiCategory.HEALTHY, BodyAnalysis.category(22.0))
        assertEquals(BmiCategory.OVERWEIGHT, BodyAnalysis.category(27.0))
        assertEquals(BmiCategory.OBESITY, BodyAnalysis.category(30.0))
    }

    @Test fun `healthy range for 150 cm`() {
        val r = BodyAnalysis.healthyWeightRange(150.0)
        assertEquals(41.6, r.start, 0.05)
        assertEquals(56.0, r.endInclusive, 0.05)
    }

    @Test fun `target below healthy bmi is rejected`() {
        val v = WeightGoalPlanner.validateTarget(89.3, 40.0, 150.0, Goal.LOSE_WEIGHT)
        assertTrue(v is TargetValidation.BelowHealthy)
        assertEquals(TargetValidation.Ok, WeightGoalPlanner.validateTarget(89.3, 70.0, 150.0, Goal.LOSE_WEIGHT))
        assertEquals(TargetValidation.WrongDirection, WeightGoalPlanner.validateTarget(80.0, 85.0, 175.0, Goal.LOSE_WEIGHT))
    }

    @Test fun `plan computes weeks date and deficit`() {
        val today = LocalDate.of(2026, 1, 1)
        val plan = WeightGoalPlanner.plan(80.0, 75.0, Goal.LOSE_WEIGHT, 0.5, today)
        assertEquals(10, plan.weeks)
        assertEquals(today.plusWeeks(10), plan.estimatedDate)
        assertEquals(-550, plan.dailyAdjustmentKcal)
        assertEquals(GoalDifficulty.EASY, plan.difficulty)
        assertEquals(GoalDifficulty.CHALLENGING, WeightGoalPlanner.plan(89.3, 52.4, Goal.LOSE_WEIGHT, 0.5, today).difficulty)
    }

    @Test fun `projection starts and ends at the right weights`() {
        val p = WeightGoalPlanner.projection(80.0, 75.0, 10)
        assertEquals(80.0, p.first(), 0.001)
        assertEquals(75.0, p.last(), 0.001)
    }

    @Test fun `water goal is rounded to cups and bounded`() {
        assertEquals(2750, Hydration.suggestedGoalMl(80.0))
        assertEquals(1500, Hydration.suggestedGoalMl(30.0))
        assertEquals(4000, Hydration.suggestedGoalMl(200.0))
    }
}
