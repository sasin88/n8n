package com.nutriai.feature.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.core.ui.components.GlassIconButton
import com.nutriai.core.ui.components.LargeTitle
import com.nutriai.core.ui.components.NoticeCard
import com.nutriai.core.ui.format.Labels
import com.nutriai.data.reminders.Notifications
import com.nutriai.data.reminders.ReminderScheduler
import com.nutriai.data.settings.ReminderSettings
import com.nutriai.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
) : ViewModel() {
    val reminders: StateFlow<ReminderSettings> = settings.settings.map { it.reminders }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReminderSettings())

    fun update(transform: (ReminderSettings) -> ReminderSettings) = viewModelScope.launch {
        val next = transform(reminders.value)
        settings.setReminders(next)
        scheduler.apply(next)
    }
}

private enum class TimeField { BREAKFAST, LUNCH, DINNER, WEIGHT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit, viewModel: NotificationsViewModel = hiltViewModel()) {
    val r by viewModel.reminders.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var denied by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<((ReminderSettings) -> ReminderSettings)?>(null) }
    var editing by remember { mutableStateOf<TimeField?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) pending?.let(viewModel::update) else denied = true
        pending = null
    }
    // Activar un recordatorio pide el permiso de notificaciones (Android 13+) solo en ese momento.
    fun enable(transform: (ReminderSettings) -> ReminderSettings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canNotify(context)) {
            pending = transform
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.update(transform)
        }
    }
    fun toggle(on: Boolean, transform: (ReminderSettings) -> ReminderSettings) = if (on) enable(transform) else viewModel.update(transform)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            GlassIconButton(onClick = onBack, contentDescription = "Volver") { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
            LargeTitle("Notificaciones")
        }
        if (denied) item { NoticeCard("Sin permiso de notificaciones no podemos avisarte. Puedes darlo en los ajustes del teléfono.", isWarning = true) }
        item {
            GlassGroup("Comidas") {
                GroupSwitch("Recordatorios de comidas", r.mealsEnabled, { v -> toggle(v) { it.copy(mealsEnabled = v) } }, "🍽️", showDivider = r.mealsEnabled)
                if (r.mealsEnabled) {
                    GroupRow("Desayuno", Labels.clock(r.breakfastMinute), onClick = { editing = TimeField.BREAKFAST })
                    GroupRow("Almuerzo", Labels.clock(r.lunchMinute), onClick = { editing = TimeField.LUNCH })
                    GroupRow("Cena", Labels.clock(r.dinnerMinute), showDivider = false, onClick = { editing = TimeField.DINNER })
                }
            }
        }
        item {
            GlassGroup("Agua") {
                GroupSwitch("Recordatorios de agua", r.waterEnabled, { v -> toggle(v) { it.copy(waterEnabled = v) } }, "💧", showDivider = r.waterEnabled)
                if (r.waterEnabled) {
                    GroupRow("Frecuencia", "Cada ${r.waterIntervalHours} h (08:00–22:00)", showDivider = false, onClick = {
                        viewModel.update { it.copy(waterIntervalHours = if (it.waterIntervalHours >= 4) 1 else it.waterIntervalHours + 1) }
                    })
                }
            }
        }
        item {
            GlassGroup("Peso") {
                GroupSwitch("Recordatorio semanal de peso", r.weightEnabled, { v -> toggle(v) { it.copy(weightEnabled = v) } }, "⚖️", showDivider = r.weightEnabled)
                if (r.weightEnabled) {
                    val day = DayOfWeek.of(r.weightDayOfWeek).getDisplayName(TextStyle.FULL, Locale("es")).replaceFirstChar { it.uppercase() }
                    GroupRow("Día", day, onClick = { viewModel.update { it.copy(weightDayOfWeek = it.weightDayOfWeek % 7 + 1) } })
                    GroupRow("Hora", Labels.clock(r.weightMinute), showDivider = false, onClick = { editing = TimeField.WEIGHT })
                }
            }
        }
        item {
            GlassGroup("Ayuno") {
                GroupSwitch("Avisar al terminar el ayuno", r.fastingEnabled, { v -> toggle(v) { it.copy(fastingEnabled = v) } }, "⏱️", showDivider = false)
            }
        }
    }

    editing?.let { field ->
        val current = when (field) {
            TimeField.BREAKFAST -> r.breakfastMinute
            TimeField.LUNCH -> r.lunchMinute
            TimeField.DINNER -> r.dinnerMinute
            TimeField.WEIGHT -> r.weightMinute
        }
        val pickerState = rememberTimePickerState(initialHour = current / 60, initialMinute = current % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { editing = null },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val m = pickerState.hour * 60 + pickerState.minute
                    viewModel.update {
                        when (field) {
                            TimeField.BREAKFAST -> it.copy(breakfastMinute = m)
                            TimeField.LUNCH -> it.copy(lunchMinute = m)
                            TimeField.DINNER -> it.copy(dinnerMinute = m)
                            TimeField.WEIGHT -> it.copy(weightMinute = m)
                        }
                    }
                    editing = null
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancelar") } },
        )
    }
}
