package com.nutriai.feature.fasting

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.core.ui.components.ConfirmDialog
import com.nutriai.core.ui.components.GlassChip
import com.nutriai.core.ui.components.LargeTitle
import com.nutriai.core.ui.components.NoticeCard
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.core.ui.components.PrimaryButton
import com.nutriai.core.ui.components.SecondaryButton
import com.nutriai.core.ui.components.ValueRow
import com.nutriai.core.ui.format.Labels
import com.nutriai.core.ui.theme.FastingColor
import com.nutriai.data.reminders.ReminderScheduler
import com.nutriai.data.repository.Clock
import com.nutriai.data.settings.SettingsRepository
import com.nutriai.domain.activity.FastingProtocol
import com.nutriai.domain.activity.FastingSession
import com.nutriai.domain.repository.FastingRepository
import com.nutriai.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FastingState(
    val profileId: Long? = null,
    val active: FastingSession? = null,
    val history: List<FastingSession> = emptyList(),
) {
    /** Días seguidos (hasta hoy) con al menos un ayuno completado. */
    fun streak(today: java.time.LocalDate): Int {
        val days = history.filter { it.completed }.map { Instant.ofEpochMilli(it.endEpochMs!!).atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()
        var d = today
        var count = 0
        if (d !in days) d = d.minusDays(1)
        while (d in days) { count++; d = d.minusDays(1) }
        return count
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FastingViewModel @Inject constructor(
    profiles: ProfileRepository,
    private val repo: FastingRepository,
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
    val clock: Clock,
) : ViewModel() {
    val state: StateFlow<FastingState> = profiles.observeActiveProfile().flatMapLatest { p ->
        if (p == null) flowOf(FastingState())
        else combine(repo.observeActive(p.id), repo.observeHistory(p.id)) { a, h -> FastingState(p.id, a, h) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FastingState())

    fun start(protocol: FastingProtocol) = viewModelScope.launch {
        val id = state.value.profileId ?: return@launch
        val session = FastingSession.start(id, protocol, clock.nowEpochMs())
        runCatching { repo.start(session) }.onSuccess {
            scheduler.scheduleFastingEnd(settings.settings.first().reminders.fastingEnabled, session.plannedEndEpochMs)
        }
    }

    fun finish() = viewModelScope.launch {
        val active = state.value.active ?: return@launch
        runCatching { repo.finish(active.id, clock.nowEpochMs()) }
        scheduler.cancelFasting()
    }

    fun delete(id: Long) = viewModelScope.launch { runCatching { repo.delete(id) } }
}

@Composable
fun FastingScreen(viewModel: FastingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.active) {
        while (state.active != null) { now = System.currentTimeMillis(); delay(1_000) }
    }
    var protocol by remember { mutableStateOf(FastingProtocol.P16_8) }
    var confirmEnd by remember { mutableStateOf(false) }
    val timeFormat = remember { DateTimeFormatter.ofPattern("EEE HH:mm", java.util.Locale("es")) }
    fun fmt(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(timeFormat)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { LargeTitle("Ayuno", subtitle = "Intermitente") }
        item {
            NutriCard {
                val active = state.active
                val progress by animateFloatAsState(active?.progress(now) ?: 0f, tween(600), label = "fast")
                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    FastingRing(progress)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (active == null) {
                            Text("¿Listo para ayunar?", style = MaterialTheme.typography.titleMedium)
                            Text(protocol.label, style = MaterialTheme.typography.displaySmall, color = FastingColor)
                            Text("${protocol.fastingHours} h de ayuno", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("Tiempo en ayuno", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Labels.duration(active.elapsedMs(now)), style = MaterialTheme.typography.displaySmall, modifier = Modifier.testTag("fast_elapsed"))
                            val remaining = active.remainingMs(now)
                            Text(
                                if (remaining > 0) "Faltan ${Labels.duration(remaining)}" else "¡Meta cumplida!",
                                style = MaterialTheme.typography.labelMedium, color = FastingColor,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (active == null) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(FastingProtocol.entries) { p -> GlassChip(p.label, p == protocol, { protocol = p }) }
                    }
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Empezar ayuno", { viewModel.start(protocol) }, modifier = Modifier.testTag("start_fast"))
                } else {
                    ValueRow("Inicio", fmt(active.startEpochMs))
                    ValueRow("Meta", fmt(active.plannedEndEpochMs))
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton("Terminar ayuno", { confirmEnd = true })
                }
            }
        }
        item {
            NutriCard {
                Text("Tu historial", style = MaterialTheme.typography.titleMedium)
                ValueRow("Racha actual", "${state.streak(viewModel.clock.today())} días")
                ValueRow("Ayunos completados", "${state.history.count { it.completed }}")
                val avg = state.history.takeIf { it.isNotEmpty() }?.map { it.elapsedMs(now) }?.average()
                ValueRow("Duración media", avg?.let { Labels.duration(it.toLong()) } ?: "—")
            }
        }
        items(state.history, key = { it.id }) { s ->
            NutriCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (s.completed) "✅" else "⏸️", style = MaterialTheme.typography.titleLarge)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("${s.protocol.label} · ${Labels.duration(s.elapsedMs(now))}", style = MaterialTheme.typography.titleMedium)
                        Text(fmt(s.startEpochMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            NoticeCard(
                title = "Aviso importante",
                text = "El ayuno intermitente no es adecuado para todas las personas. Evítalo o consulta antes con un profesional si estás embarazada o en lactancia, tienes diabetes u otra condición médica, tomas medicación, eres menor de 18 años o has tenido un trastorno de la conducta alimentaria.",
                isWarning = true,
            )
        }
    }

    if (confirmEnd) {
        ConfirmDialog(
            "¿Terminar el ayuno?", "Se guardará en tu historial con la duración actual.", "Terminar",
            onConfirm = { confirmEnd = false; viewModel.finish() }, onDismiss = { confirmEnd = false },
        )
    }
}

@Composable
private fun FastingRing(progress: Float) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    Canvas(Modifier.size(230.dp)) {
        val stroke = 18.dp.toPx()
        val inset = stroke / 2
        val arc = Size(size.width - stroke, size.height - stroke)
        drawArc(track, -90f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(FastingColor, -90f, 360f * progress, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}
