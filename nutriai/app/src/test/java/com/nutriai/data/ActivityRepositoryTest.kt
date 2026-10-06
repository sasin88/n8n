package com.nutriai.data

import com.nutriai.data.repository.ExerciseRepositoryImpl
import com.nutriai.data.repository.FastingRepositoryImpl
import com.nutriai.data.repository.RecipeRepositoryImpl
import com.nutriai.data.repository.WaterRepositoryImpl
import com.nutriai.domain.activity.ExerciseType
import com.nutriai.domain.activity.FastingProtocol
import com.nutriai.domain.activity.FastingSession
import com.nutriai.domain.model.ActivityLevel
import com.nutriai.domain.model.ExerciseLog
import com.nutriai.domain.model.FoodTag
import com.nutriai.domain.model.Goal
import com.nutriai.domain.model.RecipeComponent
import com.nutriai.domain.model.SecondaryGoal
import com.nutriai.domain.model.Sex
import com.nutriai.domain.model.UserProfile
import com.nutriai.domain.model.WaterLog
import com.nutriai.data.catalog.CatalogSeeder
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
class ActivityRepositoryTest {
    private val graph = TestGraph()
    private val water = WaterRepositoryImpl(graph.db.activityDao())
    private val exercise = ExerciseRepositoryImpl(graph.db.activityDao())
    private val fasting = FastingRepositoryImpl(graph.db.activityDao())
    private val recipes = RecipeRepositoryImpl(graph.db.foodDao(), graph.settings)

    @After fun tearDown() = graph.close()

    private suspend fun profile() = graph.profiles.create(
        UserProfile(
            name = "A", sex = Sex.FEMALE, ageYears = 30, weightKg = 65.0, heightCm = 165.0,
            activityLevel = ActivityLevel.LIGHT, goal = Goal.LOSE_WEIGHT, weeklyRateKg = 0.5,
            secondaryGoals = setOf(SecondaryGoal.MORE_ENERGY), exerciseAddsToBudget = true,
        ),
    )

    @Test fun `new profile fields persist`() = runTest {
        val id = profile()
        val p = graph.profiles.getProfile(id)!!
        assertEquals(0.5, p.weeklyRateKg!!, 0.0)
        assertEquals(setOf(SecondaryGoal.MORE_ENERGY), p.secondaryGoals)
        assertTrue(p.exerciseAddsToBudget)
    }

    @Test fun `water is summed per day and last cup can be undone`() = runTest {
        val id = profile()
        val day = graph.clock.today()
        water.add(WaterLog(profileId = id, date = day, ml = 250, createdAtEpochMs = 1))
        water.add(WaterLog(profileId = id, date = day, ml = 250, createdAtEpochMs = 2))
        assertEquals(500, water.observe(id, day, day).first().sumOf { it.ml })
        water.removeLast(id, day)
        assertEquals(250, water.observe(id, day, day).first().sumOf { it.ml })
    }

    @Test fun `exercise is stored and deleted`() = runTest {
        val id = profile()
        val day = graph.clock.today()
        val logId = exercise.add(ExerciseLog(profileId = id, date = day, type = ExerciseType.YOGA, minutes = 30, kcal = 81, estimated = true))
        assertEquals(ExerciseType.YOGA, exercise.observe(id, day, day).first().single().type)
        exercise.delete(logId)
        assertTrue(exercise.observe(id, day, day).first().isEmpty())
    }

    @Test fun `fasting session starts and finishes`() = runTest {
        val id = profile()
        val sessionId = fasting.start(FastingSession.start(id, FastingProtocol.P16_8, 1_000))
        assertEquals(FastingProtocol.P16_8, fasting.observeActive(id).first()?.protocol)
        fasting.finish(sessionId, 1_000 + 17 * 3_600_000L)
        assertNull(fasting.observeActive(id).first())
        assertTrue(fasting.observeHistory(id).first().single().completed)
    }

    @Test fun `deleting profile removes activity data`() = runTest {
        val id = profile()
        val day = graph.clock.today()
        water.add(WaterLog(profileId = id, date = day, ml = 250, createdAtEpochMs = 1))
        fasting.start(FastingSession.start(id, FastingProtocol.P14_10, 0))
        graph.profiles.delete(id)
        assertTrue(water.observe(id, day, day).first().isEmpty())
        assertNull(fasting.observeActive(id).first())
    }

    @Test fun `user recipe is computed from ingredients`() = runTest {
        profile()
        CatalogSeeder(graph.context, graph.db.foodDao(), graph.settings).seedIfNeeded()
        val rice = graph.foods.getFood("rice_white_cooked")!!
        val beans = graph.foods.getFood("beans_black_cooked")!!
        val id = recipes.saveUserRecipe("Mi pinto", listOf(RecipeComponent(rice, 100.0), RecipeComponent(beans, 100.0)), setOf(FoodTag.VEGAN))
        val r = graph.foods.getFood(id)!!
        assertTrue(r.isRecipe && r.isUserCreated)
        assertEquals(setOf(FoodTag.VEGAN), r.tags)
        val expected = (rice.nutrientsPerBasis.energyKcal + beans.nutrientsPerBasis.energyKcal) / 2
        assertEquals(expected, r.nutrientsPerBasis.energyKcal, 0.01)
        recipes.deleteUserRecipe(id)
        assertNull(graph.foods.getFood(id))
    }
}
