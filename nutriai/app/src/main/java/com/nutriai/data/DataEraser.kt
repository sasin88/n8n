package com.nutriai.data

import com.nutriai.data.image.ImageProcessor
import com.nutriai.data.local.NutriAiDatabase
import com.nutriai.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.nutriai.di.IoDispatcher

/** Borrado total de datos personales (Privacidad → Eliminar todos los datos). */
@Singleton
class DataEraser @Inject constructor(
    private val database: NutriAiDatabase,
    private val settings: SettingsRepository,
    private val images: ImageProcessor,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun eraseEverything() = withContext(io) {
        database.clearAllTables()
        // El catálogo se volverá a cargar al reiniciar la carga inicial.
        settings.clearPersonal()
        settings.setCatalogVersion(0)
        images.clearTemporaryPhotos()
    }
}
