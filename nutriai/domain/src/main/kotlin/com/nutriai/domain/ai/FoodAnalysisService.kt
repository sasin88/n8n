package com.nutriai.domain.ai

import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.result.AppResult

/**
 * Proveedor de reconocimiento de alimentos. Solo identifica alimentos y estima cantidades:
 * las calorías y macros se obtienen después de la base nutricional, no de la IA.
 *
 * Las implementaciones concretas (backend propio, mock) viven en la capa de datos,
 * de modo que cambiar de proveedor no afecta al resto de la app.
 */
interface FoodAnalysisService {
    /** Identificador legible del proveedor; las implementaciones de prueba deben indicarlo. */
    val providerName: String
    val requiresNetwork: Boolean

    suspend fun analyze(image: AnalysisImage): AppResult<FoodAnalysis>
}

class AnalysisImage(val bytes: ByteArray, val mimeType: String)

data class FoodAnalysis(
    val items: List<DetectedFood>,
    /** Confianza global entre 0 y 1. */
    val overallConfidence: Double,
    val providerName: String,
)

data class DetectedFood(
    val name: String,
    val estimatedQuantity: Double,
    val unit: PortionUnit,
    /** Confianza entre 0 y 1. */
    val confidence: Double,
)
