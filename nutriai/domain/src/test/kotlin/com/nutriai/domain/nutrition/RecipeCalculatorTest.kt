package com.nutriai.domain.nutrition

import com.nutriai.domain.TestData
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.model.RecipeComponent
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeCalculatorTest {
    private val components = listOf(
        RecipeComponent(TestData.food("a", "A", Nutrients(100.0, 2.0, 20.0, 1.0, 1.0)), 200.0),
        RecipeComponent(TestData.food("b", "B", Nutrients(200.0, 20.0, 0.0, 10.0, null)), 100.0),
    )

    @Test
    fun `total is sum of components`() {
        val total = RecipeCalculator.totalNutrients(components)
        assertEquals(400.0, total.energyKcal, 0.0001)
        assertEquals(24.0, total.proteinG, 0.0001)
        assertEquals(null, total.fiberG)
    }

    @Test
    fun `per 100 g divides by total amount`() {
        val per100 = RecipeCalculator.nutrientsPer100(components)
        assertEquals(400.0 / 3, per100.energyKcal, 0.0001)
    }
}
