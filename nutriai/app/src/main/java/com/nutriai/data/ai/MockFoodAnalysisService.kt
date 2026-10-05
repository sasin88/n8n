package com.nutriai.data.ai

import com.nutriai.domain.ai.AnalysisImage
import com.nutriai.domain.ai.FoodAnalysis
import com.nutriai.domain.ai.FoodAnalysisService
import com.nutriai.domain.ai.FoodAnalysisValidator
import com.nutriai.domain.ai.RawDetectedFood
import com.nutriai.domain.ai.RawFoodAnalysis
import com.nutriai.domain.result.AppResult
import javax.inject.Inject
import kotlinx.coroutines.delay

/**
 * MOCK — análisis simulado. NO analiza la imagen: devuelve siempre el mismo ejemplo para poder
 * probar el flujo completo mientras no haya un backend de IA conectado. La interfaz lo indica
 * con un aviso visible de "Modo demostración".
 */
class MockFoodAnalysisService @Inject constructor() : FoodAnalysisService {
    override val providerName = "Demostración (sin IA real)"
    override val requiresNetwork = false
    override val isMock = true

    override suspend fun analyze(image: AnalysisImage): AppResult<FoodAnalysis> {
        delay(1_500)
        val sample = RawFoodAnalysis(
            items = listOf(
                RawDetectedFood("Arroz blanco", 150.0, "g", 0.86),
                RawDetectedFood("Frijoles negros", 1.0, "taza", 0.74),
                RawDetectedFood("Pechuga de pollo", 120.0, "g", 0.81),
                RawDetectedFood("Ensalada de lechuga y tomate", 1.0, "porción", 0.42),
            ),
            overallConfidence = 0.71,
        )
        return FoodAnalysisValidator.validate(sample, providerName, isMock = true)
    }
}
