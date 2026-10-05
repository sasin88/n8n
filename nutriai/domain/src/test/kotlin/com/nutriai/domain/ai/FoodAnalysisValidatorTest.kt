package com.nutriai.domain.ai

import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.result.AppError
import com.nutriai.domain.result.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodAnalysisValidatorTest {

    private fun validate(vararg items: RawDetectedFood, overall: Double? = 0.8) =
        FoodAnalysisValidator.validate(RawFoodAnalysis(items.toList(), overall), "test", isMock = false)

    @Test
    fun `valid response passes`() {
        val result = validate(RawDetectedFood("Arroz", 150.0, "g", 0.9))
        val analysis = (result as AppResult.Success).value
        assertEquals(1, analysis.items.size)
        assertEquals(PortionUnit.GRAM, analysis.items[0].unit)
        assertEquals(0.8, analysis.overallConfidence, 0.0)
    }

    @Test
    fun `empty or missing items is invalid`() {
        assertEquals(AppResult.Failure(AppError.InvalidAiResponse), FoodAnalysisValidator.validate(RawFoodAnalysis(null, 0.9), "t", false))
        assertEquals(AppResult.Failure(AppError.InvalidAiResponse), validate())
    }

    @Test
    fun `items without name or with invalid confidence are dropped`() {
        val result = validate(
            RawDetectedFood(" ", 100.0, "g", 0.9),
            RawDetectedFood("Pollo", 100.0, "g", 1.7),
            RawDetectedFood("Frijoles", 100.0, "g", null),
            RawDetectedFood("Ensalada", 80.0, "gramos", 0.5),
        )
        val items = (result as AppResult.Success).value.items
        assertEquals(listOf("Ensalada"), items.map { it.name })
        assertTrue(items[0].isUncertain)
    }

    @Test
    fun `absurd quantity or unknown unit asks the user instead of guessing`() {
        val items = (validate(
            RawDetectedFood("Arroz", -5.0, "g", 0.9),
            RawDetectedFood("Pollo", 100.0, "bucket", 0.9),
        ) as AppResult.Success).value.items
        items.forEach {
            assertNull(it.estimatedQuantity)
            assertNull(it.unit)
        }
    }

    @Test
    fun `overall confidence falls back to lowest item confidence`() {
        val analysis = (validate(
            RawDetectedFood("A", 1.0, "taza", 0.9),
            RawDetectedFood("B", 1.0, "pieza", 0.4),
            overall = null,
        ) as AppResult.Success).value
        assertEquals(0.4, analysis.overallConfidence, 0.0)
    }

    @Test
    fun `spanish and english units are parsed`() {
        assertEquals(PortionUnit.CUP, FoodAnalysisValidator.parseUnit("Tazas"))
        assertEquals(PortionUnit.TABLESPOON, FoodAnalysisValidator.parseUnit("tbsp"))
        assertEquals(PortionUnit.SERVING, FoodAnalysisValidator.parseUnit("porción"))
        assertNull(FoodAnalysisValidator.parseUnit("bucket"))
    }
}
