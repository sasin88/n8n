package com.nutriai.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nutriai.domain.model.ThemeMode
import com.nutriai.domain.nutrition.EnergyFormulas
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val activeProfileId: Long? = null,
    val energyFormulaId: String = EnergyFormulas.default.id,
    val aiPhotoNoticeAccepted: Boolean = false,
    val catalogVersion: Int = 0,
)

/** Preferencias de la app en DataStore. */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            activeProfileId = prefs[ACTIVE_PROFILE],
            energyFormulaId = prefs[FORMULA] ?: EnergyFormulas.default.id,
            aiPhotoNoticeAccepted = prefs[AI_NOTICE] ?: false,
            catalogVersion = prefs[CATALOG_VERSION] ?: 0,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[THEME] = mode.name }

    suspend fun setActiveProfile(id: Long?) = dataStore.edit {
        if (id == null) it.remove(ACTIVE_PROFILE) else it[ACTIVE_PROFILE] = id
    }

    suspend fun setEnergyFormula(id: String) = dataStore.edit { it[FORMULA] = id }

    suspend fun setAiPhotoNoticeAccepted(accepted: Boolean) = dataStore.edit { it[AI_NOTICE] = accepted }

    suspend fun setCatalogVersion(version: Int) = dataStore.edit { it[CATALOG_VERSION] = version }

    /** Borra todas las preferencias salvo la versión del catálogo (que no es un dato personal). */
    suspend fun clearPersonal() = dataStore.edit { prefs ->
        val catalog = prefs[CATALOG_VERSION]
        prefs.clear()
        if (catalog != null) prefs[CATALOG_VERSION] = catalog
    }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val ACTIVE_PROFILE = longPreferencesKey("active_profile_id")
        val FORMULA = stringPreferencesKey("energy_formula_id")
        val AI_NOTICE = booleanPreferencesKey("ai_photo_notice_accepted")
        val CATALOG_VERSION = intPreferencesKey("catalog_version")
    }
}
