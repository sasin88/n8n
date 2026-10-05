package com.nutriai.domain.nutrition

import com.nutriai.domain.model.Meal
import com.nutriai.domain.model.MealItem
import com.nutriai.domain.model.MealOrigin
import com.nutriai.domain.model.MealType
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.model.NutritionSource
import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.model.WeightRecord
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NutritionStatsTest {
    private val day1 = LocalDate.of(2026, 1, 1)

    private fun meal(date: LocalDate, kcal: Double) = Meal(
        profileId = 1, date = date, type = MealType.LUNCH, origin = MealOrigin.MANUAL, createdAtEpochMs = 0,
        items = listOf(MealItem(foodId = null, name = "x", quantity = 1.0, unit = PortionUnit.GRAM, baseAmount = 1.0, nutrients = Nutrients(kcal, 0.0, 0.0, 0.0, 0.0), source = NutritionSource.USER_ENTERED)),
    )

    @Test
    fun `daily summaries include empty days`() {
        val summaries = NutritionStats.dailySummaries(
            listOf(meal(day1, 500.0), meal(day1, 300.0), meal(day1.plusDays(2), 1000.0)), day1, day1.plusDays(2),
        )
        assertEquals(listOf(800.0, 0.0, 1000.0), summaries.map { it.nutrients.energyKcal })
        assertEquals(listOf(2, 0, 1), summaries.map { it.mealCount })
    }

    @Test
    fun `average ignores days without meals`() {
        val summaries = NutritionStats.dailySummaries(listOf(meal(day1, 800.0), meal(day1.plusDays(2), 1200.0)), day1, day1.plusDays(2))
        assertEquals(1000.0, NutritionStats.averageOfLoggedDays(summaries)!!.energyKcal, 0.0001)
        assertNull(NutritionStats.averageOfLoggedDays(NutritionStats.dailySummaries(emptyList(), day1, day1)))
    }

    @Test
    fun `weight trend is kg per week`() {
        val records = listOf(
            WeightRecord(profileId = 1, date = day1, weightKg = 80.0),
            WeightRecord(profileId = 1, date = day1.plusDays(7), weightKg = 79.5),
            WeightRecord(profileId = 1, date = day1.plusDays(14), weightKg = 79.0),
        )
        assertEquals(-0.5, NutritionStats.weightTrendKgPerWeek(records)!!, 0.0001)
        assertNull(NutritionStats.weightTrendKgPerWeek(records.take(1)))
    }
}
