package com.nutriai.data.ai

import android.util.Base64
import com.nutriai.domain.ai.AnalysisImage
import com.nutriai.domain.ai.FoodAnalysis
import com.nutriai.domain.ai.FoodAnalysisService
import com.nutriai.domain.ai.FoodAnalysisValidator
import com.nutriai.domain.ai.RawDetectedFood
import com.nutriai.domain.ai.RawFoodAnalysis
import com.nutriai.domain.result.AppError
import com.nutriai.domain.result.AppResult
import java.io.IOException
import java.net.UnknownHostException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Análisis mediante el backend propio de NutriAI (ver carpeta /backend). La app nunca contiene
 * claves de proveedores de IA: solo conoce la URL de su backend.
 */
class BackendFoodAnalysisService(
    private val baseUrl: String,
    private val client: OkHttpClient,
    private val network: NetworkMonitor,
    private val ioDispatcher: CoroutineDispatcher,
) : FoodAnalysisService {
    override val providerName = "NutriAI Cloud"
    override val requiresNetwork = true
    override val isMock = false

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    override suspend fun analyze(image: AnalysisImage): AppResult<FoodAnalysis> = withContext(ioDispatcher) {
        if (!network.isOnline()) return@withContext AppResult.Failure(AppError.NoInternet)
        val body = json.encodeToString(
            AnalyzeRequest.serializer(),
            AnalyzeRequest(
                imageBase64 = Base64.encodeToString(image.bytes, Base64.NO_WRAP),
                mimeType = image.mimeType,
                locale = "es",
            ),
        )
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/v1/analyze")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 422 -> AppResult.Failure(AppError.UnreadableImage)
                    !response.isSuccessful -> AppResult.Failure(AppError.ServiceUnavailable)
                    else -> {
                        val parsed = json.decodeFromString(AnalyzeResponse.serializer(), response.body?.string().orEmpty())
                        FoodAnalysisValidator.validate(parsed.toRaw(), providerName, isMock = false)
                    }
                }
            }
        } catch (e: UnknownHostException) {
            AppResult.Failure(AppError.NoInternet)
        } catch (e: IOException) {
            AppResult.Failure(AppError.ServiceUnavailable)
        } catch (e: SerializationException) {
            AppResult.Failure(AppError.InvalidAiResponse)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.InvalidAiResponse)
        }
    }
}

@Serializable
internal data class AnalyzeRequest(
    @SerialName("image_base64") val imageBase64: String,
    @SerialName("mime_type") val mimeType: String,
    val locale: String,
)

@Serializable
internal data class AnalyzeResponse(
    val items: List<AnalyzeItem>? = null,
    @SerialName("overall_confidence") val overallConfidence: Double? = null,
) {
    fun toRaw() = RawFoodAnalysis(
        items = items?.map { RawDetectedFood(it.foodName, it.estimatedQuantity, it.unit, it.confidence) },
        overallConfidence = overallConfidence,
    )
}

@Serializable
internal data class AnalyzeItem(
    @SerialName("food_name") val foodName: String? = null,
    @SerialName("estimated_quantity") val estimatedQuantity: Double? = null,
    val unit: String? = null,
    val confidence: Double? = null,
)
