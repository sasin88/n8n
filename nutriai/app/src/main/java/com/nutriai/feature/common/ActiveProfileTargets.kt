package com.nutriai.feature.common

import com.nutriai.data.settings.SettingsRepository
import com.nutriai.domain.model.UserProfile
import com.nutriai.domain.nutrition.CalorieTarget
import com.nutriai.domain.nutrition.CalorieTargetCalculator
import com.nutriai.domain.nutrition.EnergyFormulas
import com.nutriai.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class ProfileWithTarget(val profile: UserProfile, val target: CalorieTarget)

/** Perfil activo junto con su objetivo calculado con la fórmula elegida en Configuración. */
@Singleton
class ActiveProfileTargets @Inject constructor(
    private val profiles: ProfileRepository,
    private val settings: SettingsRepository,
) {
    val flow: Flow<ProfileWithTarget?> = combine(profiles.observeActiveProfile(), settings.settings) { profile, prefs ->
        profile?.let { ProfileWithTarget(it, CalorieTargetCalculator(EnergyFormulas.byId(prefs.energyFormulaId)).calculate(it)) }
    }
}
