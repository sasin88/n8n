package com.nutriai.data

import com.nutriai.data.catalog.CatalogMapper
import com.nutriai.data.catalog.CatalogSeeder
import com.nutriai.domain.ai.FoodMatcher
import com.nutriai.domain.model.NutritionSource
import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.nutrition.PortionConverter
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Comprueba el catálogo real incluido en la app (generado desde USDA). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CatalogTest {
    private val graph = TestGraph()

    @After fun tearDown() = graph.close()

    private fun catalogText() = graph.context.assets.open("catalog/foods.json").bufferedReader().use { it.readText() }

    @Test
    fun `every food has a USDA reference and non negative values`() {
        val file = CatalogMapper.parse(catalogText())
        assertTrue(file.foods.size >= 50)
        file.foods.forEach { food ->
            assertTrue(food.id, food.sourceReference!!.startsWith("FDC "))
            assertTrue(food.id, food.per100g.kcal >= 0 && food.per100g.protein >= 0 && food.per100g.carbs >= 0 && food.per100g.fat >= 0)
        }
    }

    @Test
    fun `recipes are computed from their components`() {
        val rows = CatalogMapper.toRows(CatalogMapper.parse(catalogText()))
        val casado = rows.foods.first { it.id == "cr_casado" }
        assertEquals(NutritionSource.RECIPE, casado.source)
        assertTrue(rows.components.count { it.recipeId == "cr_casado" } >= 5)
        assertTrue(rows.portions.any { it.foodId == "cr_casado" && it.unit == PortionUnit.SERVING })
    }

    @Test
    fun `seeding loads the catalog once and enables search`() = runTest {
        val seeder = CatalogSeeder(graph.context, graph.db.foodDao(), graph.settings)
        seeder.seedIfNeeded()
        seeder.seedIfNeeded()
        val all = graph.foods.allFoods()
        assertTrue(all.size >= 55)
        val rice = FoodMatcher.bestMatch("arroz", all)?.food
        assertNotNull(rice)
        assertNotNull(PortionConverter.toBaseAmount(1.0, PortionUnit.CUP, rice!!))
        val pinto = graph.foods.search("gallo pinto").first()
        assertEquals("cr_gallo_pinto", pinto.id)
        assertTrue(pinto.isRecipe)
    }
}
