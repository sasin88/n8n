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

/** Recordatorios configurables. Horas en minutos desde medianoche. */
data class ReminderSettings(
    val mealsEnabled: Boolean = false,
    val breakfastMinute: Int = 8 * 60,
    val lunchMinute: Int = 12 * 60 + 30,
    val dinnerMinute: Int = 19 * 60,
    val waterEnabled: Boolean = false,
    val waterIntervalHours: Int = 2,
    val weightEnabled: Boolean = false,
    /** 1 = lunes … 7 = domingo (ISO). */
    val weightDayOfWeek: Int = 5,
    val weightMinute: Int = 8 * 60,
    val fastingEnabled: Boolean = false,
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val activeProfileId: Long? = null,
    val energyFormulaId: String = EnergyFormulas.default.id,
    val aiPhotoNoticeAccepted: Boolean = false,
    val catalogVersion: Int = 0,
    val reminders: ReminderSettings = ReminderSettings(),
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
            reminders = ReminderSettings(
                mealsEnabled = prefs[R_MEALS] ?: false,
                breakfastMinute = prefs[R_BREAKFAST] ?: 8 * 60,
                lunchMinute = prefs[R_LUNCH] ?: (12 * 60 + 30),
                dinnerMinute = prefs[R_DINNER] ?: 19 * 60,
                waterEnabled = prefs[R_WATER] ?: false,
                waterIntervalHours = prefs[R_WATER_INTERVAL] ?: 2,
                weightEnabled = prefs[R_WEIGHT] ?: false,
                weightDayOfWeek = prefs[R_WEIGHT_DAY] ?: 5,
                weightMinute = prefs[R_WEIGHT_MINUTE] ?: 8 * 60,
                fastingEnabled = prefs[R_FASTING] ?: false,
            ),
        )
    }

    suspend fun setReminders(r: ReminderSettings) = dataStore.edit {
        it[R_MEALS] = r.mealsEnabled
        it[R_BREAKFAST] = r.breakfastMinute
        it[R_LUNCH] = r.lunchMinute
        it[R_DINNER] = r.dinnerMinute
        it[R_WATER] = r.waterEnabled
        it[R_WATER_INTERVAL] = r.waterIntervalHours
        it[R_WEIGHT] = r.weightEnabled
        it[R_WEIGHT_DAY] = r.weightDayOfWeek
        it[R_WEIGHT_MINUTE] = r.weightMinute
        it[R_FASTING] = r.fastingEnabled
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
        val R_MEALS = booleanPreferencesKey("reminder_meals")
        val R_BREAKFAST = intPreferencesKey("reminder_breakfast")
        val R_LUNCH = intPreferencesKey("reminder_lunch")
        val R_DINNER = intPreferencesKey("reminder_dinner")
        val R_WATER = booleanPreferencesKey("reminder_water")
        val R_WATER_INTERVAL = intPreferencesKey("reminder_water_interval")
        val R_WEIGHT = booleanPreferencesKey("reminder_weight")
        val R_WEIGHT_DAY = intPreferencesKey("reminder_weight_day")
        val R_WEIGHT_MINUTE = intPreferencesKey("reminder_weight_minute")
        val R_FASTING = booleanPreferencesKey("reminder_fasting")
    }
}
