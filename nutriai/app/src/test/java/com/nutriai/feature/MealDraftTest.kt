package com.nutriai.feature

import com.nutriai.domain.ai.DetectedFood
import com.nutriai.domain.model.FoodReference
import com.nutriai.domain.model.MealOrigin
import com.nutriai.domain.model.MealType
import com.nutriai.domain.model.NutrientBasis
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.model.NutritionSource
import com.nutriai.domain.model.PortionEquivalent
import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.model.RecipeComponent
import com.nutriai.feature.meal.DraftItem
import com.nutriai.feature.meal.MealDraft
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MealDraftTest {
    private fun food(id: String, name: String, kcal: Double, aliases: List<String> = emptyList(), portions: List<PortionEquivalent> = emptyList()) =
        FoodReference(id, name, Nutrients(kcal, 1.0, 1.0, 1.0, 1.0), NutrientBasis.PER_100_G, NutritionSource.USDA_FDC, portions = portions, aliases = aliases)

    private val rice = food("rice", "Arroz blanco cocido", 130.0, listOf("arroz", "arroz blanco"), listOf(PortionEquivalent(PortionUnit.CUP, 158.0, "1 cup")))
    private val beans = food("beans", "Frijoles negros cocidos", 132.0, listOf("frijoles negros"))
    private val catalog = listOf(rice, beans)
    private var key = 0L
    private fun next() = ++key

    @Test
    fun `confident detection with known unit is ready to save`() {
        val items = MealDraft.fromDetected(listOf(DetectedFood("arroz blanco", 1.0, PortionUnit.CUP, 0.9)), catalog, ::next)
        val item = items.single()
        assertEquals("rice", item.food?.id)
        assertFalse(item.needsReview)
        assertEquals(158.0 * 1.3, item.mealItem!!.nutrients.energyKcal, 0.001)
    }

    @Test
    fun `low confidence detection needs review`() {
        val item = MealDraft.fromDetected(listOf(DetectedFood("arroz", 150.0, PortionUnit.GRAM, 0.4)), catalog, ::next).single()
        assertTrue(item.needsReview)
        assertFalse(item.isComplete)
    }

    @Test
    fun `unknown food is not matched and never gets invented values`() {
        val item = MealDraft.fromDetected(listOf(DetectedFood("sushi", 100.0, PortionUnit.GRAM, 0.95)), catalog, ::next).single()
        assertNull(item.food)
        assertNull(item.mealItem)
        assertTrue(item.needsReview)
    }

    @Test
    fun `unit not convertible for matched food asks for quantity`() {
        val item = MealDraft.fromDetected(listOf(DetectedFood("frijoles negros", 1.0, PortionUnit.CUP, 0.9)), catalog, ::next).single()
        assertEquals("beans", item.food?.id)
        assertNull(item.quantity)
        assertNull(item.mealItem)
    }

    @Test
    fun `draft can only be saved when every item is complete`() {
        val ok = DraftItem(1, rice, 100.0, PortionUnit.GRAM)
        val pending = DraftItem(2, beans, 100.0, PortionUnit.GRAM, needsReview = true)
        val draft = MealDraft(type = MealType.LUNCH, date = LocalDate.of(2026, 1, 1), origin = MealOrigin.AI_PHOTO, items = listOf(ok, pending))
        assertFalse(draft.canSave)
        assertEquals(1, draft.pendingCount)
        val confirmed = draft.copy(items = listOf(ok, pending.copy(needsReview = false)))
        assertTrue(confirmed.canSave)
        assertEquals(262.0, confirmed.total.energyKcal, 0.001)
        assertEquals(2, confirmed.toMeal(profileId = 1, nowEpochMs = 0)!!.items.size)
    }

    @Test
    fun `recipe expands into its components keeping proportions`() {
        val recipe = rice.copy(id = "casado", name = "Casado", components = listOf(RecipeComponent(rice, 150.0), RecipeComponent(beans, 100.0)))
        val item = DraftItem(1, recipe, 125.0, PortionUnit.GRAM)
        val parts = MealDraft.expandRecipe(item, ::next)!!
        assertEquals(listOf(75.0, 50.0), parts.map { it.quantity })
        assertEquals(listOf("rice", "beans"), parts.map { it.food?.id })
    }
}
