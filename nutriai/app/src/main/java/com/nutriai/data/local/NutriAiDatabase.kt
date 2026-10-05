package com.nutriai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration

@Database(
    entities = [
        ProfileEntity::class,
        FoodEntity::class,
        FoodPortionEntity::class,
        RecipeComponentEntity::class,
        MealEntity::class,
        MealItemEntity::class,
        WeightRecordEntity::class,
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

    companion object {
        const val VERSION = 1
        const val NAME = "nutriai.db"
    }
}

/**
 * Migraciones registradas. Cada cambio de esquema sube [NutriAiDatabase.VERSION], añade aquí su
 * Migration y una prueba en MigrationTest. Nunca se usa fallbackToDestructiveMigration: perder el
 * historial del usuario no es aceptable.
 */
object Migrations {
    val ALL: Array<Migration> = emptyArray()
}
