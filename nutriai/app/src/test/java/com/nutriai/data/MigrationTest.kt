package com.nutriai.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.nutriai.data.local.Migrations
import com.nutriai.data.local.NutriAiDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifica que el esquema exportado coincide con el de las entidades. Cada nueva versión
 * debe añadir aquí la prueba de su migración (helper.runMigrationsAndValidate).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), NutriAiDatabase::class.java)

    @Test
    fun `schema version 1 can be created from the exported schema`() {
        helper.createDatabase("migration-test", 1).use { db ->
            assertEquals(1, db.version)
        }
    }

    @Test
    fun `migration 1 to 2 keeps data and matches schema`() {
        helper.createDatabase("migration-12", 1).use { db ->
            db.execSQL(
                "INSERT INTO profiles (id, name, sex, ageYears, weightKg, heightCm, activityLevel, goal, unitSystem, createdAtEpochMs) " +
                    "VALUES (1, 'Adrian', 'MALE', 35, 80.0, 175.0, 'MODERATE', 'LOSE_WEIGHT', 'METRIC', 0)",
            )
        }
        helper.runMigrationsAndValidate("migration-12", 2, true, Migrations.MIGRATION_1_2).use { db ->
            db.query("SELECT name, exerciseAddsToBudget, secondaryGoals FROM profiles WHERE id = 1").use { c ->
                assert(c.moveToFirst())
                assertEquals("Adrian", c.getString(0))
                assertEquals(0, c.getInt(1))
                assertEquals("", c.getString(2))
            }
        }
    }

    @Test
    fun `every version step has a migration`() {
        val covered = Migrations.ALL.map { it.startVersion to it.endVersion }.toSet()
        (1 until NutriAiDatabase.VERSION).forEach { v -> assert((v to v + 1) in covered) { "Falta la migración $v→${v + 1}" } }
    }
}
