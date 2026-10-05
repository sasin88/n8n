package com.nutriai.domain.nutrition

import com.nutriai.domain.model.FoodReference
import com.nutriai.domain.model.NutritionSource

/** Fuente de datos nutricionales (catálogo local, API externa…). */
interface NutritionDataSource {
    val source: NutritionSource
    val requiresNetwork: Boolean

    suspend fun search(query: String, limit: Int = 20): List<FoodReference>

    suspend fun findById(id: String): FoodReference?
}
