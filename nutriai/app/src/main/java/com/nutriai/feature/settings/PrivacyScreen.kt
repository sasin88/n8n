package com.nutriai.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.core.ui.components.ConfirmDialog
import com.nutriai.core.ui.components.DangerButton
import com.nutriai.core.ui.components.NoticeCard
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.core.ui.components.SecondaryButton
import com.nutriai.data.DataEraser
import com.nutriai.data.catalog.CatalogSeeder
import com.nutriai.data.image.ImageProcessor
import com.nutriai.domain.model.UserProfile
import com.nutriai.domain.repository.MealRepository
import com.nutriai.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val meals: MealRepository,
    private val images: ImageProcessor,
    private val eraser: DataEraser,
    private val seeder: CatalogSeeder,
) : ViewModel() {
    val profile: StateFlow<UserProfile?> = profiles.observeActiveProfile().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun deletePhotos() = viewModelScope.launch {
        val count = runCatching { images.clearTemporaryPhotos() }.getOrDefault(0)
        _message.value = if (count == 0) "No había fotografías guardadas." else "Fotografías temporales eliminadas."
    }

    fun deleteHistory() = viewModelScope.launch {
        val id = profile.value?.id ?: return@launch
        _message.value = runCatching { meals.deleteAllForProfile(id) }
            .fold({ "Historial de comidas eliminado." }, { "No pudimos eliminar el historial. Inténtalo de nuevo." })
    }

    fun deleteProfile() = viewModelScope.launch {
        val id = profile.value?.id ?: return@launch
        runCatching { profiles.delete(id) }.onFailure { _message.value = "No pudimos eliminar el perfil. Inténtalo de nuevo." }
    }

    fun deleteEverything() = viewModelScope.launch {
        runCatching {
            eraser.eraseEverything()
            seeder.seedIfNeeded()
        }.onFailure { _message.value = "No pudimos eliminar todos los datos. Inténtalo de nuevo." }
    }
}

private enum class PrivacyAction { HISTORY, PROFILE, EVERYTHING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit, viewModel: PrivacyViewModel = hiltViewModel()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf<PrivacyAction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacidad y datos") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                NutriCard {
                    Text("Cómo tratamos tus datos", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "Tus perfiles, comidas y pesos se guardan solo en este teléfono.",
                        "No hay cuentas, emails ni contraseñas.",
                        "Las fotos se usan solo para el análisis y se borran al terminar; no se guardan en tu galería.",
                        "Con la IA conectada, la foto se envía a nuestro servidor para analizarla y no se almacena allí.",
                        "Los datos no se incluyen en copias de seguridad en la nube.",
                    ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp)) }
                }
            }
            message?.let { item { NoticeCard(it) } }
            item { SecondaryButton("Eliminar fotografías temporales", viewModel::deletePhotos) }
            profile?.let { p ->
                item { DangerButton("Eliminar historial de ${p.name}", { confirm = PrivacyAction.HISTORY }) }
                item { DangerButton("Eliminar perfil de ${p.name}", { confirm = PrivacyAction.PROFILE }) }
            }
            item { DangerButton("Eliminar todos los datos", { confirm = PrivacyAction.EVERYTHING }) }
        }
    }

    when (confirm) {
        PrivacyAction.HISTORY -> ConfirmDialog(
            "¿Eliminar historial?", "Se borrarán todas las comidas de este perfil. No se puede deshacer.", "Eliminar",
            onConfirm = { confirm = null; viewModel.deleteHistory() }, onDismiss = { confirm = null }, destructive = true,
        )
        PrivacyAction.PROFILE -> ConfirmDialog(
            "¿Eliminar perfil?", "Se borrarán el perfil y todos sus datos (comidas, peso, alimentos propios). No se puede deshacer.", "Eliminar",
            onConfirm = { confirm = null; viewModel.deleteProfile() }, onDismiss = { confirm = null }, destructive = true,
        )
        PrivacyAction.EVERYTHING -> ConfirmDialog(
            "¿Eliminar todos los datos?", "Se borrarán todos los perfiles, comidas, pesos, ajustes y fotos de este teléfono. No se puede deshacer.", "Eliminar todo",
            onConfirm = { confirm = null; viewModel.deleteEverything() }, onDismiss = { confirm = null }, destructive = true,
        )
        null -> Unit
    }
}
