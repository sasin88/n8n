package com.nutriai.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.nutriai.domain.model.ActivityLevel
import com.nutriai.domain.model.Goal
import com.nutriai.domain.model.Sex
import com.nutriai.domain.model.UserProfile
import com.nutriai.domain.repository.ProfileRepository
import com.nutriai.navigation.ProfileFormRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileFormState(
    val isEditing: Boolean = false,
    val name: String = "",
    val sex: Sex? = null,
    val age: String = "",
    val weight: String = "",
    val height: String = "",
    val activity: ActivityLevel? = null,
    val goal: Goal? = null,
    val targetWeight: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    val validation: ProfileValidation get() = ProfileValidation.validate(this)
}

/** Validación del formulario, separada de la UI para poder probarla. */
data class ProfileValidation(
    val nameError: String?,
    val ageError: String?,
    val weightError: String?,
    val heightError: String?,
    val targetWeightError: String?,
    val isMinor: Boolean,
) {
    val isValid get() = listOf(nameError, ageError, weightError, heightError, targetWeightError).all { it == null }

    companion object {
        fun parse(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

        fun validate(s: ProfileFormState): ProfileValidation {
            val age = s.age.trim().toIntOrNull()
            val weight = parse(s.weight)
            val height = parse(s.height)
            val target = s.targetWeight.takeIf { it.isNotBlank() }?.let(::parse)
            return ProfileValidation(
                nameError = when {
                    s.name.isBlank() -> "Escribe un nombre o apodo"
                    s.name.trim().length > 30 -> "Máximo 30 caracteres"
                    else -> null
                },
                ageError = if (age == null || age !in 13..100) "Edad entre 13 y 100 años" else null,
                weightError = if (weight == null || weight !in 25.0..300.0) "Peso entre 25 y 300 kg" else null,
                heightError = if (height == null || height !in 100.0..250.0) "Altura entre 100 y 250 cm" else null,
                targetWeightError = if (s.targetWeight.isNotBlank() && (target == null || target !in 25.0..300.0)) "Peso entre 25 y 300 kg" else null,
                isMinor = age != null && age < 18,
            )
        }
    }
}

@HiltViewModel
class ProfileFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val profiles: ProfileRepository,
) : ViewModel() {
    private val profileId = savedStateHandle.toRoute<ProfileFormRoute>().profileId.takeIf { it >= 0 }
    private var original: UserProfile? = null

    private val _state = MutableStateFlow(ProfileFormState(isEditing = profileId != null))
    val state: StateFlow<ProfileFormState> = _state.asStateFlow()

    init {
        if (profileId != null) viewModelScope.launch {
            profiles.getProfile(profileId)?.let { p ->
                original = p
                _state.value = ProfileFormState(
                    isEditing = true, name = p.name, sex = p.sex, age = p.ageYears.toString(),
                    weight = p.weightKg.clean(), height = p.heightCm.clean(), activity = p.activityLevel,
                    goal = p.goal, targetWeight = p.targetWeightKg?.clean().orEmpty(),
                )
            }
        }
    }

    fun update(transform: (ProfileFormState) -> ProfileFormState) = _state.update(transform)

    fun save() {
        val s = _state.value
        if (!s.validation.isValid || s.sex == null || s.activity == null || s.goal == null || s.saving) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val profile = (original ?: UserProfile(
                name = "", sex = s.sex, ageYears = 0, weightKg = 0.0, heightCm = 0.0,
                activityLevel = s.activity, goal = s.goal,
            )).copy(
                name = s.name.trim(),
                sex = s.sex,
                ageYears = s.age.trim().toInt(),
                weightKg = ProfileValidation.parse(s.weight)!!,
                heightCm = ProfileValidation.parse(s.height)!!,
                activityLevel = s.activity,
                goal = s.goal,
                targetWeightKg = s.targetWeight.takeIf { it.isNotBlank() }?.let(ProfileValidation::parse),
            )
            runCatching { if (original == null) profiles.create(profile) else profiles.update(profile) }
                .onSuccess { _state.update { it.copy(saving = false, saved = true) } }
                .onFailure { _state.update { it.copy(saving = false, error = "No pudimos guardar el perfil. Inténtalo de nuevo.") } }
        }
    }

    private fun Double.clean() = if (this % 1.0 == 0.0) toInt().toString() else toString()
}
