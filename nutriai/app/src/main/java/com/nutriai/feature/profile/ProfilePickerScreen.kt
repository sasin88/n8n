package com.nutriai.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.core.ui.components.NutriCard
import com.nutriai.core.ui.components.SecondaryButton
import com.nutriai.core.ui.format.Labels
import com.nutriai.domain.model.UserProfile
import com.nutriai.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfilePickerState(val profiles: List<UserProfile> = emptyList(), val activeId: Long? = null)

@HiltViewModel
class ProfilePickerViewModel @Inject constructor(private val repository: ProfileRepository) : ViewModel() {
    val state: StateFlow<ProfilePickerState> =
        combine(repository.observeProfiles(), repository.observeActiveProfile()) { all, active ->
            ProfilePickerState(all, active?.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilePickerState())

    fun select(id: Long, then: () -> Unit) {
        viewModelScope.launch {
            repository.setActive(id)
            then()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePickerScreen(
    onBack: () -> Unit,
    onCreateNew: () -> Unit,
    viewModel: ProfilePickerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cambiar de perfil") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Cada perfil tiene sus propios datos, comidas y estadísticas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.profiles, key = { it.id }) { profile ->
                NutriCard(onClick = { viewModel.select(profile.id, onBack) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.name.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(profile.name, style = MaterialTheme.typography.titleMedium)
                            Text(Labels.goal(profile.goal), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (profile.id == state.activeId) Icon(Icons.Default.Check, "Perfil activo", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item { SecondaryButton("+ Crear otro perfil", onCreateNew) }
        }
    }
}
