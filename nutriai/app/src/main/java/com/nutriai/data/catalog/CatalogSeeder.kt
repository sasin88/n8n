package com.nutriai.data.catalog

import android.content.Context
import com.nutriai.data.local.FoodDao
import com.nutriai.data.local.FoodEntity
import com.nutriai.data.local.FoodPortionEntity
import com.nutriai.data.local.RecipeComponentEntity
import com.nutriai.data.settings.SettingsRepository
import com.nutriai.domain.model.FoodCategory
import com.nutriai.domain.model.NutrientBasis
import com.nutriai.domain.model.Nutrients
import com.nutriai.domain.model.NutritionSource
import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.model.RecipeComponent
import com.nutriai.domain.nutrition.RecipeCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CatalogFile(
    val version: Int,
    val source: String,
    val foods: List<CatalogFood>,
    val recipes: List<CatalogRecipe>,
)

@Serializable
data class CatalogFood(
    val id: String,
    val name: String,
    val category: String,
    val aliases: List<String> = emptyList(),
    val source: String,
    val sourceReference: String? = null,
    val per100g: CatalogNutrients,
    val portions: List<CatalogPortion> = emptyList(),
)

@Serializable
data class CatalogNutrients(val kcal: Double, val protein: Double, val carbs: Double, val fat: Double, val fiber: Double? = null)

@Serializable
data class CatalogPortion(val unit: String, val grams: Double, val label: String)

@Serializable
data class CatalogRecipe(
    val id: String,
    val name: String,
    val countryCode: String? = null,
    val aliases: List<String> = emptyList(),
    val components: List<CatalogComponent>,
)

@Serializable
data class CatalogComponent(val foodId: String, val grams: Double)

/** Convierte el catálogo JSON (generado desde USDA) en filas de Room. */
object CatalogMapper {
    const val RECIPE_REFERENCE = "Receta aproximada de NutriAI; valores de ingredientes de USDA FoodData Central"
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): CatalogFile = json.decodeFromString(CatalogFile.serializer(), text)

    data class Rows(val foods: List<FoodEntity>, val portions: List<FoodPortionEntity>, val components: List<RecipeComponentEntity>)

    fun toRows(file: CatalogFile): Rows {
        val foods = file.foods.map { food ->
            FoodEntity(
                id = food.id,
                name = food.name,
                aliases = food.aliases.joinToString("|"),
                category = enumOr(food.category, FoodCategory.OTHER),
                basis = NutrientBasis.PER_100_G,
                energyKcal = food.per100g.kcal,
                proteinG = food.per100g.protein,
                carbohydratesG = food.per100g.carbs,
                fatG = food.per100g.fat,
                fiberG = food.per100g.fiber,
                source = enumOr(food.source, NutritionSource.USDA_FDC),
                sourceReference = food.sourceReference,
                countryCode = null,
                ownerProfileId = null,
            )
        }
        val portions = file.foods.flatMap { food ->
            food.portions.mapNotNull { p ->
                val unit = runCatching { PortionUnit.valueOf(p.unit) }.getOrNull() ?: return@mapNotNull null
                if (p.grams <= 0) null else FoodPortionEntity(food.id, unit, p.grams, p.label)
            }
        }
        val byId = foods.associateBy { it.id }

        val recipeFoods = mutableListOf<FoodEntity>()
        val recipePortions = mutableListOf<FoodPortionEntity>()
        val components = mutableListOf<RecipeComponentEntity>()
        for (recipe in file.recipes) {
            val parts = recipe.components.mapNotNull { c -> byId[c.foodId]?.let { RecipeComponent(it.toReferenceStub(), c.grams) } }
            if (parts.size != recipe.components.size || parts.isEmpty()) continue
            val per100 = RecipeCalculator.nutrientsPer100(parts)
            val total = RecipeCalculator.totalAmount(parts)
            recipeFoods += FoodEntity(
                id = recipe.id,
                name = recipe.name,
                aliases = recipe.aliases.joinToString("|"),
                category = FoodCategory.DISHES,
                basis = NutrientBasis.PER_100_G,
                energyKcal = per100.energyKcal,
                proteinG = per100.proteinG,
                carbohydratesG = per100.carbohydratesG,
                fatG = per100.fatG,
                fiberG = per100.fiberG,
                source = NutritionSource.RECIPE,
                sourceReference = RECIPE_REFERENCE,
                countryCode = recipe.countryCode,
                ownerProfileId = null,
            )
            recipePortions += FoodPortionEntity(recipe.id, PortionUnit.SERVING, total, "1 plato (receta aproximada)")
            components += recipe.components.map { RecipeComponentEntity(recipe.id, it.foodId, it.grams) }
        }
        return Rows(foods + recipeFoods, portions + recipePortions, components)
    }

    private inline fun <reified E : Enum<E>> enumOr(value: String, default: E): E =
        runCatching { enumValueOf<E>(value) }.getOrDefault(default)
}

/** Carga el catálogo incluido en la app la primera vez o cuando cambia su versión. */
@Singleton
class CatalogSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val foodDao: FoodDao,
    private val settings: SettingsRepository,
) {
    suspend fun seedIfNeeded() {
        val text = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        val file = CatalogMapper.parse(text)
        if (settings.current().catalogVersion >= file.version) return
        val rows = CatalogMapper.toRows(file)
        foodDao.replaceCatalog(rows.foods, rows.portions, rows.components)
        settings.setCatalogVersion(file.version)
    }

    private companion object {
        const val ASSET_PATH = "catalog/foods.json"
    }
}
