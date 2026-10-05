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
    fun `every version step has a migration`() {
        val covered = Migrations.ALL.map { it.startVersion to it.endVersion }.toSet()
        (1 until NutriAiDatabase.VERSION).forEach { v -> assert((v to v + 1) in covered) { "Falta la migración $v→${v + 1}" } }
    }
}
