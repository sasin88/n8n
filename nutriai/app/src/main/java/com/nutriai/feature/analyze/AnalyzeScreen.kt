package com.nutriai.feature.analyze

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.core.ui.components.AppearIn
import com.nutriai.core.ui.components.NoticeCard
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.data.ai.NetworkMonitor
import com.nutriai.data.settings.SettingsRepository
import com.nutriai.domain.ai.FoodAnalysisService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AnalyzeViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val service: FoodAnalysisService,
    private val network: NetworkMonitor,
) : ViewModel() {
    val noticeAccepted: StateFlow<Boolean?> = settings.settings.map { it.aiPhotoNoticeAccepted }
        .stateIn<Boolean?>(viewModelScope, SharingStarted.Eagerly, null)
    val isMock: Boolean get() = service.isMock
    fun needsInternetButOffline(): Boolean = service.requiresNetwork && !network.isOnline()

    fun acceptNotice() = viewModelScope.launch { settings.setAiPhotoNoticeAccepted(true) }
}

private enum class PendingAction { CAMERA, GALLERY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeScreen(
    onBack: () -> Unit,
    onCamera: () -> Unit,
    onImagePicked: (Uri) -> Unit,
    onManual: () -> Unit,
    viewModel: AnalyzeViewModel = hiltViewModel(),
) {
    val accepted by viewModel.noticeAccepted.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<PendingAction?>(null) }
    var offline by remember { mutableStateOf(false) }
    // Photo Picker del sistema: no requiere permiso de almacenamiento.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onImagePicked) }
    val launchGallery = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    fun start(action: PendingAction) {
        if (viewModel.needsInternetButOffline()) { offline = true; return }
        if (accepted != true) { pending = action; return }
        if (action == PendingAction.CAMERA) onCamera() else launchGallery()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analizar comida") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (viewModel.isMock) {
                NoticeCard(
                    title = "Modo demostración",
                    text = "La IA real todavía no está conectada. Puedes probar el flujo completo, pero el resultado será simulado.",
                    isWarning = true,
                )
            } else {
                NoticeCard("El análisis por foto necesita conexión a Internet.")
            }
            if (offline) NoticeCard("Sin conexión a Internet no podemos analizar fotos. Puedes registrar la comida manualmente.", isWarning = true)
            AppearIn(0) { Option("📷", "Tomar una foto", "Usa la cámara para fotografiar tu plato.") { start(PendingAction.CAMERA) } }
            AppearIn(1) { Option("🖼️", "Elegir de la galería", "Selecciona una foto que ya tengas.") { start(PendingAction.GALLERY) } }
            AppearIn(2) { Option("✍️", "Escribir lo que comí", "Busca los alimentos y elige las cantidades.", onManual) }
            Text(
                "Consejo: fotografía el plato desde arriba, con buena luz y sin otros objetos.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    pending?.let { action ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Antes de analizar") },
            text = {
                Text(
                    if (viewModel.isMock) {
                        "En modo demostración la foto no sale de tu teléfono y el resultado es simulado. La foto se borra al terminar."
                    } else {
                        "La foto se enviará a nuestro servidor solo para identificar los alimentos y no se guarda. Las calorías serán una estimación que podrás corregir."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.acceptNotice()
                    pending = null
                    if (action == PendingAction.CAMERA) onCamera() else launchGallery()
                }) { Text("Entendido") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Option(emoji: String, title: String, body: String, onClick: () -> Unit) {
    NutriCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
