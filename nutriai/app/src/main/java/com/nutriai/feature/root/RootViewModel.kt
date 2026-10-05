package com.nutriai.feature.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriai.data.catalog.CatalogSeeder
import com.nutriai.data.image.ImageProcessor
import com.nutriai.data.settings.SettingsRepository
import com.nutriai.domain.model.ThemeMode
import com.nutriai.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RootState {
    data object Loading : RootState
    data class Ready(val hasProfile: Boolean, val themeMode: ThemeMode) : RootState
}

@HiltViewModel
class RootViewModel @Inject constructor(
    profiles: ProfileRepository,
    settings: SettingsRepository,
    private val seeder: CatalogSeeder,
    private val images: ImageProcessor,
) : ViewModel() {
    private val catalogReady = MutableStateFlow(false)

    val state: StateFlow<RootState> = combine(catalogReady, profiles.observeActiveProfile(), settings.settings) { ready, profile, prefs ->
        if (!ready) RootState.Loading else RootState.Ready(hasProfile = profile != null, themeMode = prefs.themeMode)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RootState.Loading)

    init {
        viewModelScope.launch {
            // Si la carga del catálogo fallara, la app sigue funcionando con alimentos propios.
            runCatching { seeder.seedIfNeeded() }
            // Fotos temporales de sesiones anteriores (p. ej. si la app se cerró a mitad de un análisis).
            runCatching { images.clearTemporaryPhotos() }
            catalogReady.value = true
        }
    }

    /** Recarga el catálogo tras un borrado total de datos. */
    fun reseedCatalog() {
        viewModelScope.launch { runCatching { seeder.seedIfNeeded() } }
    }
}
