package com.nutriai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProfileEntity::class,
        FoodEntity::class,
        FoodPortionEntity::class,
        RecipeComponentEntity::class,
        MealEntity::class,
        MealItemEntity::class,
        WeightRecordEntity::class,
        WaterLogEntity::class,
        ExerciseLogEntity::class,
        FastingSessionEntity::class,
    ],
    version = NutriAiDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NutriAiDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun foodDao(): FoodDao
    abstract fun mealDao(): MealDao
    abstract fun weightDao(): WeightDao
    abstract fun activityDao(): ActivityDao

    companion object {
        const val VERSION = 2
        const val NAME = "nutriai.db"
    }
}

/**
 * Migraciones registradas. Cada cambio de esquema sube [NutriAiDatabase.VERSION], añade aquí su
 * Migration y una prueba en MigrationTest. Nunca se usa fallbackToDestructiveMigration: perder el
 * historial del usuario no es aceptable.
 */
object Migrations {
    /** v2: agua, ejercicio, ayuno, campos de plan del perfil y etiquetas de recetas. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `profiles` ADD COLUMN `secondaryGoals` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `profiles` ADD COLUMN `weeklyRateKg` REAL")
            db.execSQL("ALTER TABLE `profiles` ADD COLUMN `waterGoalMl` INTEGER")
            db.execSQL("ALTER TABLE `profiles` ADD COLUMN `exerciseAddsToBudget` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `foods` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `water_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`profileId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `ml` INTEGER NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_water_logs_profileId_date` ON `water_logs` (`profileId`, `date`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `exercise_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`profileId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `type` TEXT NOT NULL, `minutes` INTEGER NOT NULL, " +
                    "`kcal` INTEGER NOT NULL, `estimated` INTEGER NOT NULL, `note` TEXT, " +
                    "FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_logs_profileId_date` ON `exercise_logs` (`profileId`, `date`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `fasting_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`profileId` INTEGER NOT NULL, `protocol` TEXT NOT NULL, `startEpochMs` INTEGER NOT NULL, " +
                    "`plannedEndEpochMs` INTEGER NOT NULL, `endEpochMs` INTEGER, " +
                    "FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_fasting_sessions_profileId` ON `fasting_sessions` (`profileId`)")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
