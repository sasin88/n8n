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
import com.nutriai.domain.model.WeightRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileRepositoryTest {
    private val graph = TestGraph()

    @After fun tearDown() = graph.close()

    private val adrian = UserProfile(
        name = "Adrian", sex = Sex.MALE, ageYears = 35, weightKg = 80.0, heightCm = 175.0,
        activityLevel = ActivityLevel.MODERATE, goal = Goal.LOSE_WEIGHT,
    )
    private val maria = UserProfile(
        name = "María", sex = Sex.FEMALE, ageYears = 30, weightKg = 65.0, heightCm = 165.0,
        activityLevel = ActivityLevel.LIGHT, goal = Goal.MAINTAIN_WEIGHT,
    )

    private fun meal(profileId: Long, kcal: Double) = Meal(
        profileId = profileId, date = graph.clock.today(), type = MealType.LUNCH, origin = MealOrigin.MANUAL, createdAtEpochMs = 1,
        items = listOf(MealItem(foodId = null, name = "x", quantity = 100.0, unit = PortionUnit.GRAM, baseAmount = 100.0,
            nutrients = Nutrients(kcal, 1.0, 1.0, 1.0, null), source = NutritionSource.USER_ENTERED)),
    )

    @Test
    fun `creating a profile makes it active`() = runTest {
        val id = graph.profiles.create(adrian)
        assertEquals(id, graph.profiles.observeActiveProfile().first()?.id)
        assertEquals("Adrian", graph.profiles.getProfile(id)?.name)
    }

    @Test
    fun `switching profiles changes the active one`() = runTest {
        val a = graph.profiles.create(adrian)
        val m = graph.profiles.create(maria)
        assertEquals(m, graph.profiles.observeActiveProfile().first()?.id)
        graph.profiles.setActive(a)
        assertEquals("Adrian", graph.profiles.observeActiveProfile().first()?.name)
        assertEquals(2, graph.profiles.observeProfiles().first().size)
    }

    @Test
    fun `profiles keep their data separate`() = runTest {
        val a = graph.profiles.create(adrian)
        val m = graph.profiles.create(maria)
        graph.meals.save(meal(a, 500.0))
        graph.meals.save(meal(m, 300.0))
        val day = graph.clock.today()
        assertEquals(listOf(500.0), graph.meals.observeMeals(a, day, day).first().map { it.nutrients.energyKcal })
        assertEquals(listOf(300.0), graph.meals.observeMeals(m, day, day).first().map { it.nutrients.energyKcal })
    }

    @Test
    fun `deleting a profile removes its meals and weights and picks another active profile`() = runTest {
        val a = graph.profiles.create(adrian)
        val m = graph.profiles.create(maria)
        graph.meals.save(meal(m, 300.0))
        graph.weights.add(WeightRecord(profileId = m, date = graph.clock.today(), weightKg = 64.0))

        graph.profiles.delete(m)

        val day = graph.clock.today()
        assertTrue(graph.meals.observeMeals(m, day, day).first().isEmpty())
        assertTrue(graph.weights.observeRecords(m).first().isEmpty())
        assertEquals(a, graph.profiles.observeActiveProfile().first()?.id)
    }

    @Test
    fun `deleting the last profile leaves no active profile`() = runTest {
        val a = graph.profiles.create(adrian)
        graph.profiles.delete(a)
        assertNull(graph.profiles.observeActiveProfile().first())
    }

    @Test
    fun `adding a weight record updates the profile weight`() = runTest {
        val a = graph.profiles.create(adrian)
        graph.weights.add(WeightRecord(profileId = a, date = graph.clock.today(), weightKg = 79.5))
        assertEquals(79.5, graph.profiles.getProfile(a)!!.weightKg, 0.0)
    }
}
