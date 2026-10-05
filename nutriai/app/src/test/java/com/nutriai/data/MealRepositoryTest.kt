package com.nutriai.data

import com.nutriai.domain.model.ActivityLevel
import com.nutriai.domain.model.Goal
import com.nutriai.domain.model.Meal
import com.nutriai.domain.model.MealItem
import com.nutriai.domain.model.MealOrigin
import com.nutriai.domain.model.MealType
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.model.NutritionSource
import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.model.Sex
import com.nutriai.domain.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MealRepositoryTest {
    private val graph = TestGraph()

    @After fun tearDown() = graph.close()

    private suspend fun profile() = graph.profiles.create(
        UserProfile(name = "A", sex = Sex.MALE, ageYears = 30, weightKg = 70.0, heightCm = 170.0, activityLevel = ActivityLevel.LIGHT, goal = Goal.MAINTAIN_WEIGHT),
    )

    private fun item(name: String, kcal: Double, fiber: Double? = 1.0) = MealItem(
        foodId = "f_$name", name = name, quantity = 100.0, unit = PortionUnit.GRAM, baseAmount = 100.0,
        nutrients = Nutrients(kcal, 10.0, 20.0, 5.0, fiber), source = NutritionSource.USDA_FDC, aiConfidence = 0.8,
    )

    @Test
    fun `saved meal is read back with items in order`() = runTest {
        val p = profile()
        val id = graph.meals.save(
            Meal(profileId = p, date = graph.clock.today(), type = MealType.DINNER, origin = MealOrigin.AI_PHOTO,
                analysisConfidence = 0.7, createdAtEpochMs = 5, items = listOf(item("arroz", 195.0), item("pollo", 198.0, null))),
        )
        val meal = graph.meals.getMeal(id)!!
        assertEquals(listOf("arroz", "pollo"), meal.items.map { it.name })
        assertEquals(393.0, meal.nutrients.energyKcal, 0.001)
        assertNull(meal.nutrients.fiberG)
        assertEquals(0.7, meal.analysisConfidence!!, 0.0)
        assertEquals(MealOrigin.AI_PHOTO, meal.origin)
    }

    @Test
    fun `editing a meal replaces its items`() = runTest {
        val p = profile()
        val id = graph.meals.save(Meal(profileId = p, date = graph.clock.today(), type = MealType.LUNCH, origin = MealOrigin.MANUAL, createdAtEpochMs = 1, items = listOf(item("a", 100.0), item("b", 100.0))))
        val existing = graph.meals.getMeal(id)!!
        graph.meals.save(existing.copy(items = listOf(item("c", 50.0))))
        assertEquals(listOf("c"), graph.meals.getMeal(id)!!.items.map { it.name })
    }

    @Test
    fun `deleting a meal removes it`() = runTest {
        val p = profile()
        val id = graph.meals.save(Meal(profileId = p, date = graph.clock.today(), type = MealType.SNACK, origin = MealOrigin.MANUAL, createdAtEpochMs = 1, items = listOf(item("a", 100.0))))
        graph.meals.delete(id)
        assertNull(graph.meals.getMeal(id))
        val day = graph.clock.today()
        assertEquals(0, graph.meals.observeMeals(p, day, day).first().size)
    }

    @Test
    fun `meals are filtered by date range`() = runTest {
        val p = profile()
        val today = graph.clock.today()
        graph.meals.save(Meal(profileId = p, date = today, type = MealType.LUNCH, origin = MealOrigin.MANUAL, createdAtEpochMs = 1, items = listOf(item("hoy", 100.0))))
        graph.meals.save(Meal(profileId = p, date = today.minusDays(3), type = MealType.LUNCH, origin = MealOrigin.MANUAL, createdAtEpochMs = 1, items = listOf(item("antes", 100.0))))
        assertEquals(listOf("hoy"), graph.meals.observeMeals(p, today, today).first().flatMap { it.items }.map { it.name })
        assertEquals(2, graph.meals.observeMeals(p, today.minusDays(7), today).first().size)
    }

    @Test
    fun `data persists after reopening the database`() = runTest {
        val p = profile()
        val id = graph.meals.save(Meal(profileId = p, date = graph.clock.today(), type = MealType.LUNCH, origin = MealOrigin.MANUAL, createdAtEpochMs = 1, items = listOf(item("a", 123.0))))
        graph.db.close()
        val reopened = graph.open()
        try {
            assertEquals(123.0, reopened.mealDao().getItems(id).single().energyKcal, 0.0)
            assertEquals("A", reopened.profileDao().get(p)!!.name)
        } finally {
            reopened.close()
        }
    }
}
