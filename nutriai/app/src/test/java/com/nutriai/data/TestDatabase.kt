package com.nutriai.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nutriai.data.local.NutriAiDatabase
import com.nutriai.data.repository.Clock
import com.nutriai.data.repository.FoodRepositoryImpl
import com.nutriai.data.repository.MealRepositoryImpl
import com.nutriai.data.repository.ProfileRepositoryImpl
import com.nutriai.data.repository.WeightRepositoryImpl
import com.nutriai.data.settings.SettingsRepository
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class FixedClock(var date: LocalDate = LocalDate.of(2026, 3, 10), var now: Long = 1_000L) : Clock {
    override fun nowEpochMs(): Long = now++
    override fun today(): LocalDate = date
}

/** Base de datos y repositorios reales sobre archivos temporales. */
class TestGraph(name: String = "test-${System.nanoTime()}") {
    val context: Context = ApplicationProvider.getApplicationContext()
    private val dbFile = context.getDatabasePath("$name.db")
    private val prefsFile = File(context.filesDir, "$name.preferences_pb")
    val clock = FixedClock()

    val db: NutriAiDatabase = open()
    val settings = SettingsRepository(
        PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + SupervisorJob())) { prefsFile },
    )
    val profiles = ProfileRepositoryImpl(db.profileDao(), settings, clock)
    val meals = MealRepositoryImpl(db.mealDao())
    val weights = WeightRepositoryImpl(db.weightDao(), db.profileDao())
    val foods = FoodRepositoryImpl(db.foodDao(), settings)

    fun open(): NutriAiDatabase =
        Room.databaseBuilder(context, NutriAiDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .build()

    fun close() = db.close()
}
